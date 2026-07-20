package com.ecommerce.order.application;

import com.ecommerce.order.domain.Order;
import com.ecommerce.order.domain.OrderItem;
import com.ecommerce.order.client.ProductGatewayClient;
import com.ecommerce.order.dto.CreateOrderItemRequest;
import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.dto.PaymentCreateResponse;
import com.ecommerce.order.exception.OrderIntegrationException;
import com.ecommerce.order.exception.OrderNotFoundException;
import com.ecommerce.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.time.LocalDateTime;

@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderRepository orderRepository;
    private final OrderSagaOrchestrator orderSagaOrchestrator;
    private final ProductGatewayClient productGatewayClient;

    public OrderServiceImpl(OrderRepository orderRepository,
                            OrderSagaOrchestrator orderSagaOrchestrator,
                            ProductGatewayClient productGatewayClient) {
        this.orderRepository = orderRepository;
        this.orderSagaOrchestrator = orderSagaOrchestrator;
        this.productGatewayClient = productGatewayClient;
    }

    @Override
    @Transactional(noRollbackFor = OrderIntegrationException.class)
    public OrderResponse createOrder(CreateOrderRequest request) {
        // Product la nguon gia hien tai; khong tin gia FE gui len.
        productGatewayClient.validateProducts(request);

        Order order = new Order(
                request.userId(),
                request.buyerName(),
                request.buyerEmail(),
                normalizeDescription(request.description()),
                normalizeCurrency(request.currency()),
                normalizeShippingFee(request.shippingFee()),
                request.items().stream().map(this::toOrderItem).toList()
        );
        // Luu don truoc khi goi service khac de co orderId cho Inventory va Payment.
        order = orderRepository.save(order);

        try {
            // Thu tu Saga: giu hang -> tao payment -> chuyen don sang cho thanh toan.
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
            // Neu mot buoc lien service loi, hoan tac phan giu hang da lam truoc do.
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
        // Event co the gui lai; chi xu ly don dang pending de tranh tru kho 2 lan.
        if (order.getStatus() != com.ecommerce.order.domain.OrderStatus.PAYMENT_PENDING) {
            log.warn("Ignoring late payment success for order {} with status {}", orderId, order.getStatus());
            return;
        }
        // Payment OK -> Inventory tru kho that -> don duoc xac nhan.
        orderSagaOrchestrator.confirmInventoryFor(order);
        order.markConfirmed();
        orderRepository.save(order);
    }

    @Override
    @Transactional
    public void handlePaymentFailure(UUID orderId, String reason, boolean cancelled) {
        Order order = findOrder(orderId);
        // Chi don pending moi duoc phep fail/huy va tra kho.
        if (order.getStatus() != com.ecommerce.order.domain.OrderStatus.PAYMENT_PENDING) {
            log.warn("Ignoring payment failure for order {} with status {}", orderId, order.getStatus());
            return;
        }
        safelyReleaseInventory(order);
        if (cancelled) {
            order.markCancelled(reason);
        } else {
            order.markFailed(reason);
        }
        orderRepository.save(order);
    }

    @Override
    @Transactional
    public void cancelExpiredPaymentOrders() {
        // Don pending qua 1 phut can tra hang de nguoi khac mua duoc.
        LocalDateTime expiredBefore = LocalDateTime.now().minusMinutes(1);
        List<Order> expiredOrders = orderRepository.findByStatusAndCreatedAtBefore(
                com.ecommerce.order.domain.OrderStatus.PAYMENT_PENDING,
                expiredBefore
        );

        for (Order order : expiredOrders) {
            try {
                // Chi huy DB sau khi Inventory da nhan yeu cau tra hang; neu loi se retry lan sau.
                orderSagaOrchestrator.releaseInventoryFor(order);
                order.markCancelled("Payment was not completed within 1 minute");
                orderRepository.save(order);
                log.info("Cancelled expired unpaid order {} and released inventory", order.getId());
            } catch (OrderIntegrationException exception) {
                log.error("Could not release inventory for expired order {}; will retry", order.getId(), exception);
            }
        }
    }

    private Order findOrder(UUID orderId) {
        return orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    private OrderItem toOrderItem(CreateOrderItemRequest request) {
        return new OrderItem(
                request.productId(),
                request.productName(),
                request.size(),
                request.color(),
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

    private java.math.BigDecimal normalizeShippingFee(java.math.BigDecimal shippingFee) {
        return shippingFee == null ? java.math.BigDecimal.ZERO : shippingFee;
    }

    private void safelyReleaseInventory(Order order) {
        try {
            orderSagaOrchestrator.releaseInventoryFor(order);
        } catch (OrderIntegrationException ignored) {
            // Don van duoc danh dau loi; van hanh sau se xu ly viec tra kho neu can.
        }
    }
}
