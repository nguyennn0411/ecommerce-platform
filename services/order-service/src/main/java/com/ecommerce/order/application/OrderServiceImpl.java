package com.ecommerce.order.application;

import com.ecommerce.order.domain.Order;
import com.ecommerce.order.domain.OrderItem;
import com.ecommerce.order.dto.CreateOrderItemRequest;
import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.dto.PaymentCreateResponse;
import com.ecommerce.order.exception.OrderIntegrationException;
import com.ecommerce.order.exception.OrderNotFoundException;
import com.ecommerce.order.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderSagaOrchestrator orderSagaOrchestrator;

    public OrderServiceImpl(OrderRepository orderRepository,
                            OrderSagaOrchestrator orderSagaOrchestrator) {
        this.orderRepository = orderRepository;
        this.orderSagaOrchestrator = orderSagaOrchestrator;
    }

    @Override
    @Transactional(noRollbackFor = OrderIntegrationException.class)
    public OrderResponse createOrder(CreateOrderRequest request) {
        Order order = new Order(
                request.userId(),
                request.buyerName(),
                request.buyerEmail(),
                normalizeDescription(request.description()),
                normalizeCurrency(request.currency()),
                request.items().stream().map(this::toOrderItem).toList()
        );
        order = orderRepository.save(order);

        try {
            orderSagaOrchestrator.reserveInventoryFor(order);
            PaymentCreateResponse payment = orderSagaOrchestrator.createPaymentFor(order);
            order.markPaymentPending(
                    payment.paymentId(),
                    payment.orderCode(),
                    payment.paymentLinkId(),
                    payment.checkoutUrl(),
                    payment.qrCode()
            );
            return OrderResponse.from(orderRepository.save(order));
        } catch (OrderIntegrationException exception) {
            safelyReleaseInventory(order);
            order.markFailed(exception.getMessage());
            orderRepository.save(order);
            throw exception;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID orderId) {
        return OrderResponse.from(findOrder(orderId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByUser(UUID userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(OrderResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public void handlePaymentSuccess(UUID orderId) {
        Order order = findOrder(orderId);
        order.markConfirmed();
        orderRepository.save(order);
    }

    @Override
    @Transactional
    public void handlePaymentFailure(UUID orderId, String reason, boolean cancelled) {
        Order order = findOrder(orderId);
        safelyReleaseInventory(order);
        if (cancelled) {
            order.markCancelled(reason);
        } else {
            order.markFailed(reason);
        }
        orderRepository.save(order);
    }

    private Order findOrder(UUID orderId) {
        return orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    private OrderItem toOrderItem(CreateOrderItemRequest request) {
        return new OrderItem(
                request.productId(),
                request.productName(),
                request.quantity(),
                request.unitPrice()
        );
    }

    private String normalizeCurrency(String currency) {
        return currency == null || currency.isBlank() ? "VND" : currency.toUpperCase(Locale.ROOT);
    }

    private String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return "Order created from order-service";
        }
        return description.trim();
    }

    private void safelyReleaseInventory(Order order) {
        try {
            orderSagaOrchestrator.releaseInventoryFor(order);
        } catch (OrderIntegrationException ignored) {
            // Keep the order state transition even if compensation cannot complete immediately.
        }
    }
}
