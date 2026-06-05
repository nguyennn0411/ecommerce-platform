package com.ecommerce.order.application;

import com.ecommerce.order.api.dto.CancelOrderRequest;
import com.ecommerce.order.api.dto.CompletePaymentRequest;
import com.ecommerce.order.api.dto.CreateOrderItemRequest;
import com.ecommerce.order.api.dto.CreateOrderRequest;
import com.ecommerce.order.api.dto.OrderItemResponse;
import com.ecommerce.order.api.dto.OrderResponse;
import com.ecommerce.order.api.dto.SagaStepResponse;
import com.ecommerce.order.api.dto.SagaTransactionResponse;
import com.ecommerce.order.api.dto.UpdateOrderStatusRequest;
import com.ecommerce.order.domain.Order;
import com.ecommerce.order.domain.OrderItem;
import com.ecommerce.order.domain.OrderStatus;
import com.ecommerce.order.domain.SagaStep;
import com.ecommerce.order.domain.SagaTransaction;
import com.ecommerce.order.persistence.OrderRepository;
import com.ecommerce.order.persistence.SagaTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class OrderUseCase {

    private static final DateTimeFormatter ORDER_CODE_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final OrderRepository orderRepository;
    private final SagaTransactionRepository sagaTransactionRepository;
    private final OrderSagaOrchestrator sagaOrchestrator;

    public OrderUseCase(
            OrderRepository orderRepository,
            SagaTransactionRepository sagaTransactionRepository,
            OrderSagaOrchestrator sagaOrchestrator
    ) {
        this.orderRepository = orderRepository;
        this.sagaTransactionRepository = sagaTransactionRepository;
        this.sagaOrchestrator = sagaOrchestrator;
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        Order order = new Order();
        order.setOrderCode(generateOrderCode());
        order.setUserId(request.userId());
        order.setCurrency(resolveCurrency(request.currency()));
        order.setShippingAddress(request.shippingAddress());
        order.setNote(request.note());
        order.setStatus(OrderStatus.PENDING);

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (CreateOrderItemRequest itemRequest : request.items()) {
            BigDecimal subtotal = itemRequest.unitPrice().multiply(BigDecimal.valueOf(itemRequest.quantity()));

            OrderItem item = new OrderItem();
            item.setProductId(itemRequest.productId());
            item.setProductName(itemRequest.productName());
            item.setUnitPrice(itemRequest.unitPrice());
            item.setQuantity(itemRequest.quantity());
            item.setSubtotal(subtotal);

            order.addItem(item);
            totalAmount = totalAmount.add(subtotal);
        }
        order.setTotalAmount(totalAmount);

        Order savedOrder = orderRepository.save(order);
        sagaOrchestrator.startCreateOrderSaga(savedOrder);
        return toOrderResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID orderId) {
        return toOrderResponse(findOrder(orderId));
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderByCode(String orderCode) {
        return orderRepository.findByOrderCode(orderCode)
                .map(this::toOrderResponse)
                .orElseThrow(() -> new IllegalArgumentException("Order not found for code: " + orderCode));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> searchOrders(UUID userId, OrderStatus status) {
        List<Order> orders;
        if (userId != null && status != null) {
            orders = orderRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status);
        } else if (userId != null) {
            orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
        } else if (status != null) {
            orders = orderRepository.findByStatusOrderByCreatedAtDesc(status);
        } else {
            orders = orderRepository.findAll();
        }
        return orders.stream().map(this::toOrderResponse).toList();
    }

    @Transactional
    public OrderResponse updateOrderStatus(UUID orderId, UpdateOrderStatusRequest request) {
        Order order = findOrder(orderId);
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.COMPLETED) {
            throw new IllegalArgumentException("Cannot update a terminal order status: " + order.getStatus());
        }
        order.setStatus(request.status());
        return toOrderResponse(order);
    }

    @Transactional
    public OrderResponse cancelOrder(UUID orderId, CancelOrderRequest request) {
        Order order = findOrder(orderId);
        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw new IllegalArgumentException("Completed orders cannot be cancelled");
        }
        order.setStatus(OrderStatus.CANCELLED);
        sagaOrchestrator.compensateCancelledOrder(order, request.reason());
        return toOrderResponse(order);
    }

    @Transactional
    public OrderResponse completePayment(UUID orderId, CompletePaymentRequest request) {
        Order order = findOrder(orderId);
        sagaOrchestrator.handlePaymentProcessed(order, request);
        return toOrderResponse(order);
    }

    @Transactional(readOnly = true)
    public List<SagaTransactionResponse> getSagaTransactions(UUID orderId) {
        if (!orderRepository.existsById(orderId)) {
            throw new IllegalArgumentException("Order not found: " + orderId);
        }
        return sagaTransactionRepository.findByOrder_IdOrderByCreatedAtDesc(orderId)
                .stream()
                .map(this::toSagaTransactionResponse)
                .toList();
    }

    private Order findOrder(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
    }

    private String generateOrderCode() {
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        return "SZ-" + ORDER_CODE_TIMESTAMP.format(LocalDateTime.now()) + "-" + suffix;
    }

    private String resolveCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return "VND";
        }
        return currency.trim().toUpperCase(Locale.ROOT);
    }

    private OrderResponse toOrderResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getId(),
                        item.getProductId(),
                        item.getProductName(),
                        item.getUnitPrice(),
                        item.getQuantity(),
                        item.getSubtotal()
                ))
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getOrderCode(),
                order.getUserId(),
                order.getTotalAmount(),
                order.getCurrency(),
                order.getStatus(),
                order.getShippingAddress(),
                order.getPaymentId(),
                order.getNote(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                items
        );
    }

    private SagaTransactionResponse toSagaTransactionResponse(SagaTransaction transaction) {
        List<SagaStepResponse> steps = transaction.getSteps().stream()
                .map(this::toSagaStepResponse)
                .toList();
        return new SagaTransactionResponse(
                transaction.getId(),
                transaction.getSagaType(),
                transaction.getCurrentStep(),
                transaction.getStatus(),
                transaction.getErrorMessage(),
                transaction.getCreatedAt(),
                transaction.getUpdatedAt(),
                steps
        );
    }

    private SagaStepResponse toSagaStepResponse(SagaStep step) {
        return new SagaStepResponse(
                step.getId(),
                step.getStepName(),
                step.getStatus(),
                step.getRequestPayload(),
                step.getResponsePayload(),
                step.getErrorMessage(),
                step.getCreatedAt(),
                step.getUpdatedAt()
        );
    }
}
