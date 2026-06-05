package com.ecommerce.order.application;

import com.ecommerce.order.api.dto.CompletePaymentRequest;
import com.ecommerce.order.domain.Order;
import com.ecommerce.order.domain.OrderItem;
import com.ecommerce.order.domain.OrderStatus;
import com.ecommerce.order.domain.SagaStep;
import com.ecommerce.order.domain.SagaStepStatus;
import com.ecommerce.order.domain.SagaTransaction;
import com.ecommerce.order.domain.SagaTransactionStatus;
import com.ecommerce.order.integration.InventoryAdjustmentResponse;
import com.ecommerce.order.integration.InventoryClient;
import com.ecommerce.order.integration.InventoryReservationResponse;
import com.ecommerce.order.integration.ProductCatalogClient;
import com.ecommerce.order.integration.ProductValidationResponse;
import com.ecommerce.order.integration.ReserveInventoryItemRequest;
import com.ecommerce.order.integration.ReserveInventoryRequest;
import com.ecommerce.order.integration.ValidateProductItemRequest;
import com.ecommerce.order.integration.ValidateProductsRequest;
import com.ecommerce.order.messaging.OrderEventPublisher;
import com.ecommerce.order.persistence.SagaTransactionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrderSagaOrchestrator {

    private static final String CREATE_ORDER_SAGA = "CREATE_ORDER";
    private static final String PAYMENT_RESULT_SAGA = "PAYMENT_RESULT";
    private static final String CANCEL_ORDER_SAGA = "CANCEL_ORDER";

    private final SagaTransactionRepository sagaTransactionRepository;
    private final ProductCatalogClient productCatalogClient;
    private final InventoryClient inventoryClient;
    private final OrderEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final boolean externalServicesEnabled;

    public OrderSagaOrchestrator(
            SagaTransactionRepository sagaTransactionRepository,
            ProductCatalogClient productCatalogClient,
            InventoryClient inventoryClient,
            OrderEventPublisher eventPublisher,
            ObjectMapper objectMapper,
            @Value("${ecommerce.saga.external-services-enabled:false}") boolean externalServicesEnabled
    ) {
        this.sagaTransactionRepository = sagaTransactionRepository;
        this.productCatalogClient = productCatalogClient;
        this.inventoryClient = inventoryClient;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
        this.externalServicesEnabled = externalServicesEnabled;
    }

    @Transactional
    public SagaTransaction startCreateOrderSaga(Order order) {
        SagaTransaction saga = createSaga(order, CREATE_ORDER_SAGA);
        boolean inventoryReserved = false;

        try {
            runStep(saga, "VALIDATE_ORDER_REQUEST", order.getId(), () -> "Order payload accepted");
            runStep(saga, "VALIDATE_PRODUCTS", toValidateProductsRequest(order), () -> validateProducts(order));
            runStep(saga, "RESERVE_INVENTORY", toReserveInventoryRequest(order), () -> reserveInventory(order));
            inventoryReserved = true;

            order.setStatus(OrderStatus.PENDING_PAYMENT);
            runStep(saga, "CREATE_ORDER", order.getId(), () -> "Order persisted and moved to PENDING_PAYMENT");
            runStep(saga, "PUBLISH_ORDER_CREATED", order.getId(), () -> eventPublisher.publishOrderCreated(order));

            saga.setCurrentStep("WAITING_FOR_PAYMENT");
            saga.setStatus(SagaTransactionStatus.COMPLETED);
        } catch (RuntimeException exception) {
            order.setStatus(OrderStatus.FAILED);
            saga.setStatus(SagaTransactionStatus.FAILED);
            saga.setErrorMessage(exception.getMessage());

            if (inventoryReserved) {
                compensateInventoryRelease(saga, order, "Create order saga failed after stock reservation");
            }
        }

        return sagaTransactionRepository.save(saga);
    }

    @Transactional
    public SagaTransaction handlePaymentProcessed(Order order, CompletePaymentRequest request) {
        SagaTransaction saga = createSaga(order, PAYMENT_RESULT_SAGA);

        try {
            runStep(saga, "RECORD_PAYMENT_RESULT", request, () -> request.paymentStatus());

            if (isSuccessfulPayment(request.paymentStatus())) {
                order.setPaymentId(request.paymentId());
                order.setStatus(OrderStatus.PAID);
                runStep(saga, "CONFIRM_INVENTORY_DEDUCTION", order.getId(), () -> confirmInventory(order));
                runStep(saga, "PUBLISH_PAYMENT_PROCESSED", request,
                        () -> eventPublisher.publishPaymentProcessed(order, request.paymentStatus(), request.transactionId()));

                saga.setCurrentStep("ORDER_PAID");
                saga.setStatus(SagaTransactionStatus.COMPLETED);
            } else {
                order.setPaymentId(request.paymentId());
                order.setStatus(OrderStatus.CANCELLED);
                runStep(saga, "RELEASE_INVENTORY", order.getId(), () -> releaseInventory(order, "Payment failed"));
                runStep(saga, "PUBLISH_PAYMENT_PROCESSED", request,
                        () -> eventPublisher.publishPaymentProcessed(order, request.paymentStatus(), request.transactionId()));

                saga.setCurrentStep("PAYMENT_FAILED_COMPENSATED");
                saga.setStatus(SagaTransactionStatus.COMPENSATED);
            }
        } catch (RuntimeException exception) {
            saga.setStatus(SagaTransactionStatus.FAILED);
            saga.setErrorMessage(exception.getMessage());
        }

        return sagaTransactionRepository.save(saga);
    }

    @Transactional
    public SagaTransaction compensateCancelledOrder(Order order, String reason) {
        SagaTransaction saga = createSaga(order, CANCEL_ORDER_SAGA);

        try {
            runStep(saga, "RELEASE_INVENTORY", order.getId(), () -> releaseInventory(order, reason));
            runStep(saga, "MARK_ORDER_CANCELLED", order.getId(), () -> "Order cancelled: " + reason);
            saga.setCurrentStep("ORDER_CANCELLED");
            saga.setStatus(SagaTransactionStatus.COMPENSATED);
        } catch (RuntimeException exception) {
            saga.setStatus(SagaTransactionStatus.FAILED);
            saga.setErrorMessage(exception.getMessage());
        }

        return sagaTransactionRepository.save(saga);
    }

    private SagaTransaction createSaga(Order order, String sagaType) {
        SagaTransaction saga = new SagaTransaction();
        saga.setOrder(order);
        saga.setSagaType(sagaType);
        saga.setCurrentStep("STARTED");
        saga.setStatus(SagaTransactionStatus.STARTED);
        return sagaTransactionRepository.save(saga);
    }

    private Object runStep(SagaTransaction saga, String stepName, Object request, SagaStepAction action) {
        saga.setCurrentStep(stepName);

        SagaStep step = new SagaStep();
        step.setStepName(stepName);
        step.setStatus(SagaStepStatus.RUNNING);
        step.setRequestPayload(toJson(request));
        saga.addStep(step);

        try {
            Object response = action.execute();
            step.setResponsePayload(toJson(response));
            step.setStatus(SagaStepStatus.SUCCESS);
            return response;
        } catch (RuntimeException exception) {
            step.setStatus(SagaStepStatus.FAILED);
            step.setErrorMessage(exception.getMessage());
            throw exception;
        } catch (Exception exception) {
            step.setStatus(SagaStepStatus.FAILED);
            step.setErrorMessage(exception.getMessage());
            throw new IllegalStateException(exception.getMessage(), exception);
        }
    }

    private ProductValidationResponse validateProducts(Order order) {
        if (!externalServicesEnabled) {
            return new ProductValidationResponse(true, "Simulated product validation; external services disabled");
        }

        ProductValidationResponse response = productCatalogClient.validateProducts(toValidateProductsRequest(order));
        if (response == null || !response.valid()) {
            String message = response == null ? "Product validation returned no response" : response.message();
            throw new IllegalArgumentException(message);
        }
        return response;
    }

    private InventoryReservationResponse reserveInventory(Order order) {
        if (!externalServicesEnabled) {
            return new InventoryReservationResponse(true, "Simulated inventory reservation; external services disabled");
        }

        InventoryReservationResponse response = inventoryClient.reserveStock(toReserveInventoryRequest(order));
        if (response == null || !response.reserved()) {
            String message = response == null ? "Inventory reservation returned no response" : response.message();
            throw new IllegalArgumentException(message);
        }
        return response;
    }

    private InventoryAdjustmentResponse confirmInventory(Order order) {
        if (!externalServicesEnabled) {
            return new InventoryAdjustmentResponse(true, "Simulated inventory confirmation; external services disabled");
        }
        return inventoryClient.confirmReservation(order.getId());
    }

    private InventoryAdjustmentResponse releaseInventory(Order order, String reason) {
        if (!externalServicesEnabled) {
            return new InventoryAdjustmentResponse(true, "Simulated inventory release; reason=" + reason);
        }
        return inventoryClient.releaseReservation(order.getId(), reason);
    }

    private void compensateInventoryRelease(SagaTransaction saga, Order order, String reason) {
        try {
            Object response = releaseInventory(order, reason);
            SagaStep step = new SagaStep();
            step.setStepName("COMPENSATE_RELEASE_INVENTORY");
            step.setStatus(SagaStepStatus.COMPENSATED);
            step.setRequestPayload(toJson(order.getId()));
            step.setResponsePayload(toJson(response));
            saga.addStep(step);
            saga.setStatus(SagaTransactionStatus.COMPENSATED);
            saga.setCurrentStep("COMPENSATE_RELEASE_INVENTORY");
        } catch (RuntimeException compensationException) {
            saga.setErrorMessage(saga.getErrorMessage() + "; compensation failed: " + compensationException.getMessage());
        }
    }

    private ValidateProductsRequest toValidateProductsRequest(Order order) {
        List<ValidateProductItemRequest> items = order.getItems().stream()
                .map(item -> new ValidateProductItemRequest(
                        item.getProductId(),
                        item.getUnitPrice(),
                        item.getQuantity()
                ))
                .toList();
        return new ValidateProductsRequest(items);
    }

    private ReserveInventoryRequest toReserveInventoryRequest(Order order) {
        List<ReserveInventoryItemRequest> items = order.getItems().stream()
                .map(this::toReserveInventoryItemRequest)
                .toList();
        return new ReserveInventoryRequest(order.getId(), items);
    }

    private ReserveInventoryItemRequest toReserveInventoryItemRequest(OrderItem item) {
        return new ReserveInventoryItemRequest(item.getProductId(), item.getQuantity());
    }

    private boolean isSuccessfulPayment(String paymentStatus) {
        return switch (paymentStatus.trim().toUpperCase()) {
            case "PAID", "SUCCESS", "SUCCEEDED", "COMPLETED", "APPROVED" -> true;
            default -> false;
        };
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            return String.valueOf(value);
        }
    }

    @FunctionalInterface
    private interface SagaStepAction {
        Object execute() throws Exception;
    }
}
