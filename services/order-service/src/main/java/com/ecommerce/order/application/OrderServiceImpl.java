package com.ecommerce.order.application;

import com.ecommerce.order.domain.Order;
import com.ecommerce.order.domain.OrderItem;
import com.ecommerce.order.domain.OrderStatus;
import com.ecommerce.order.domain.SagaLogStatus;
import com.ecommerce.order.client.ProductGatewayClient;
import com.ecommerce.order.dto.CreateOrderItemRequest;
import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.dto.PaymentCreateResponse;
import com.ecommerce.order.dto.SagaTransactionLogResponse;
import com.ecommerce.order.exception.OrderIntegrationException;
import com.ecommerce.order.exception.OrderNotFoundException;
import com.ecommerce.order.repository.OrderRepository;
import com.ecommerce.order.messaging.OrderNotificationPublisher;
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
    private final SagaLogService sagaLogService;
    private final OrderNotificationPublisher notificationPublisher;

    public OrderServiceImpl(OrderRepository orderRepository,
                            OrderSagaOrchestrator orderSagaOrchestrator,
                            ProductGatewayClient productGatewayClient,
                            SagaLogService sagaLogService,
                            OrderNotificationPublisher notificationPublisher) {
        this.orderRepository = orderRepository;
        this.orderSagaOrchestrator = orderSagaOrchestrator;
        this.productGatewayClient = productGatewayClient;
        this.sagaLogService = sagaLogService;
        this.notificationPublisher = notificationPublisher;
    }

    // Tạo đơn mới: kiểm tra Product, giữ hàng Inventory, tạo Payment và trả link PayOS cho FE.
    @Override
    @Transactional(noRollbackFor = OrderIntegrationException.class)
    public OrderResponse createOrder(CreateOrderRequest request) {
        // Product là nguồn giá hiện tại; không tin giá FE gửi lên.
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
        // Lưu đơn trước khi gọi service khác để có orderId cho Inventory và Payment.
        order = orderRepository.save(order);
        sagaLogService.write(order.getId(), "ORDER_CREATED", SagaLogStatus.SUCCESS, "Order saved after product validation");
        sagaLogService.write(order.getId(), "PRODUCT_VALIDATED", SagaLogStatus.SUCCESS, "Product Catalog accepted products and prices");

        try {
            // 3. Gọi Inventory giữ hàng tạm, chưa trừ kho thật
            orderSagaOrchestrator.reserveInventoryFor(order);
            sagaLogService.write(order.getId(), "INVENTORY_RESERVED", SagaLogStatus.SUCCESS, "Inventory reservation created");

            // 4. Gọi Payment tạo giao dịch và lấy link PayOS.
            PaymentCreateResponse payment = orderSagaOrchestrator.createPaymentFor(order);
            sagaLogService.write(order.getId(), "PAYMENT_CREATED", SagaLogStatus.SUCCESS, "PayOS payment link created");

            // 5. Lưu paymentId/link vào đơn, chuyển sang chờ thanh toán.
            order.markPaymentPending(
                    payment.paymentId(),
                    payment.orderCode(),
                    payment.paymentLinkId(),
                    payment.checkoutUrl(),
                    payment.qrCode()
            );
            order = orderRepository.save(order);
            notificationPublisher.publish(order, "created", "Đơn hàng đang chờ thanh toán");
            return OrderResponse.from(order);
        } catch (OrderIntegrationException exception) {
            // Có lỗi ở Inventory hoặc Payment thì trả hàng đã giữ.
            sagaLogService.write(order.getId(), "ORDER_CREATION", SagaLogStatus.FAILED, exception.getMessage());
            safelyReleaseInventory(order);
            order.markFailed(exception.getMessage());
            order = orderRepository.save(order);
            notificationPublisher.publish(order, "failed", "Không thể tạo đơn hàng: " + exception.getMessage());
            throw exception;
        }
    }

    // Lấy chi tiết một đơn theo orderId để FE hoặc staff đọc trạng thái hiện tại.
    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID orderId) {
        return OrderResponse.from(findOrder(orderId));
    }

    // Lấy lịch sử đơn của một user, dùng cho màn My Orders.
    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByUser(UUID userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(OrderResponse::from)
                .toList();
    }

    // Lấy danh sách đơn cho staff, có thể lọc theo trạng thái nếu truyền status.
    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders(OrderStatus status) {
        List<Order> orders = status == null
                ? orderRepository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"))
                : orderRepository.findByStatusOrderByCreatedAtDesc(status);
        return orders.stream().map(OrderResponse::from).toList();
    }

    // Lấy các bước Saga đã ghi lại để hiển thị quá trình xử lý đơn.
    @Override
    @Transactional(readOnly = true)
    public List<SagaTransactionLogResponse> getSagaLogs(UUID orderId) {
        findOrder(orderId);
        return sagaLogService.findByOrderId(orderId);
    }

    // Customer hủy đơn của chính mình khi đơn vẫn đang chờ thanh toán.
    @Override
    @Transactional
    public OrderResponse cancelOrder(UUID orderId, UUID userId, String reason) {
        Order order = findOrder(orderId);
        if (!order.getUserId().equals(userId)) {
            throw new IllegalStateException("You cannot cancel another user's order");
        }
        return cancelPendingOrder(order, reason == null || reason.isBlank() ? "Cancelled by customer" : reason.trim());
    }

    // Staff hủy đơn đang chờ thanh toán thay cho khách hoặc khi cần xử lý vận hành.
    @Override
    @Transactional
    public OrderResponse cancelOrderByStaff(UUID orderId, String reason) {
        return cancelPendingOrder(findOrder(orderId), reason == null || reason.isBlank() ? "Cancelled by staff" : reason.trim());
    }

    // Staff chốt đơn hoàn thành sau khi đơn đang giao đã giao thành công.
    @Override
    @Transactional
    public OrderResponse completeOrderByStaff(UUID orderId) {
        Order order = findOrder(orderId);
        if (order.getStatus() != OrderStatus.SHIPPING) {
            throw new IllegalStateException("Only shipping orders can be completed");
        }
        order.markCompleted();
        order = orderRepository.save(order);
        sagaLogService.write(order.getId(), "ORDER_COMPLETED", SagaLogStatus.SUCCESS, "Staff completed order after delivery confirmation");
        notificationPublisher.publish(order, "completed", "Đơn hàng đã được nhân viên chốt hoàn thành");
        return OrderResponse.from(order);
    }

    // Staff chuyển đơn đã thanh toán sang trạng thái đang giao.
    @Override
    @Transactional
    public OrderResponse markShippingByStaff(UUID orderId) {
        Order order = findOrder(orderId);
        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Only paid orders can be moved to shipping");
        }
        order.markShipping();
        order = orderRepository.save(order);
        sagaLogService.write(order.getId(), "ORDER_SHIPPING", SagaLogStatus.SUCCESS, "Staff handed order to shipping");
        notificationPublisher.publish(order, "shipping", "Đơn hàng đang được giao");
        return OrderResponse.from(order);
    }

    // Staff đánh dấu đơn đang giao bị hoàn về cửa hàng.
    @Override
    @Transactional
    public OrderResponse returnOrderByStaff(UUID orderId, String reason) {
        Order order = findOrder(orderId);
        if (order.getStatus() != OrderStatus.SHIPPING) {
            throw new IllegalStateException("Only shipping orders can be returned");
        }
        String normalizedReason = reason == null || reason.isBlank() ? "Returned after failed delivery" : reason.trim();
        order.markReturned(normalizedReason);
        order = orderRepository.save(order);
        sagaLogService.write(order.getId(), "ORDER_RETURNED", SagaLogStatus.COMPENSATED, normalizedReason);
        notificationPublisher.publish(order, "returned", "Đơn hàng giao không thành công và đã hoàn về");
        return OrderResponse.from(order);
    }

    // Nhận payment.success từ RabbitMQ, xác nhận kho đã trừ hàng và chuyển đơn sang CONFIRMED.
    @Override
    @Transactional
    public void handlePaymentSuccess(UUID orderId) {
        Order order = findOrder(orderId);
        // Event có thể gửi lại; chỉ xử lý đơn đang pending để tránh trừ kho 2 lần.
        if (order.getStatus() != com.ecommerce.order.domain.OrderStatus.PAYMENT_PENDING) {
            log.warn("Ignoring late payment success for order {} with status {}", orderId, order.getStatus());
            return;
        }
        sagaLogService.write(orderId, "PAYMENT_SUCCESS_RECEIVED", SagaLogStatus.SUCCESS, "Received payment.success event");
        // Payment OK -> Inventory trừ kho thật -> đơn được xác nhận.
        orderSagaOrchestrator.confirmInventoryFor(order);
        sagaLogService.write(orderId, "INVENTORY_CONFIRMED", SagaLogStatus.SUCCESS, "Inventory deducted after payment success");
        order.markConfirmed();
        order = orderRepository.save(order);
        sagaLogService.write(orderId, "ORDER_CONFIRMED", SagaLogStatus.SUCCESS, "Order confirmed");
        notificationPublisher.publish(order, "confirmed", "Thanh toán thành công, đơn hàng đã được xác nhận");
    }

    // Nhận payment.failed hoặc payment.cancelled từ RabbitMQ, trả hàng đã giữ và đóng đơn.
    @Override
    @Transactional
    public void handlePaymentFailure(UUID orderId, String reason, boolean cancelled) {
        Order order = findOrder(orderId);
        // Chỉ đơn pending mới được phép fail/hủy và trả kho.
        if (order.getStatus() != com.ecommerce.order.domain.OrderStatus.PAYMENT_PENDING) {
            log.warn("Ignoring payment failure for order {} with status {}", orderId, order.getStatus());
            return;
        }
        sagaLogService.write(orderId, cancelled ? "PAYMENT_CANCELLED" : "PAYMENT_FAILED", SagaLogStatus.FAILED, reason);
        safelyReleaseInventory(order);
        if (cancelled) {
            order.markCancelled(reason);
        } else {
            order.markFailed(reason);
        }
        order = orderRepository.save(order);
        notificationPublisher.publish(order, cancelled ? "cancelled" : "failed", order.getFailureReason());
    }

    // Tự hủy các đơn chờ thanh toán quá hạn để giải phóng hàng đã giữ trong Inventory.
    @Override
    @Transactional
    public void cancelExpiredPaymentOrders() {
        // Đơn pending quá 1 phút cần trả hàng để người khác mua được.
        LocalDateTime expiredBefore = LocalDateTime.now().minusMinutes(1);
        List<Order> expiredOrders = orderRepository.findByStatusAndCreatedAtBefore(
                com.ecommerce.order.domain.OrderStatus.PAYMENT_PENDING,
                expiredBefore
        );

        for (Order order : expiredOrders) {
            try {
                // Chỉ hủy DB sau khi Inventory đã nhận yêu cầu trả hàng; nếu lỗi sẽ retry lần sau.
                orderSagaOrchestrator.releaseInventoryFor(order);
                sagaLogService.write(order.getId(), "INVENTORY_RELEASED", SagaLogStatus.COMPENSATED, "Reservation released after payment timeout");
                order.markCancelled("Payment was not completed within 1 minute");
                order = orderRepository.save(order);
                notificationPublisher.publish(order, "cancelled", order.getFailureReason());
                log.info("Cancelled expired unpaid order {} and released inventory", order.getId());
            } catch (OrderIntegrationException exception) {
                log.error("Could not release inventory for expired order {}; will retry", order.getId(), exception);
            }
        }
    }

    // Tìm Order trong DB; nếu không có thì ném lỗi 404 qua GlobalExceptionHandler.
    private Order findOrder(UUID orderId) {
        return orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    // Chuyển item request từ FE sang entity OrderItem để lưu vào DB.
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

    // Chuẩn hóa tiền tệ; mặc định là VND nếu FE không gửi.
    private String normalizeCurrency(String currency) {
        return currency == null || currency.isBlank() ? "VND" : currency.toUpperCase(Locale.ROOT);
    }

    // Chuẩn hóa mô tả đơn để Payment luôn có nội dung giao dịch.
    private String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return "Order created from order-service";
        }
        return description.trim();
    }

    // Chuẩn hóa phí ship; nếu FE không gửi thì mặc định bằng 0.
    private java.math.BigDecimal normalizeShippingFee(java.math.BigDecimal shippingFee) {
        return shippingFee == null ? java.math.BigDecimal.ZERO : shippingFee;
    }

    // Cố gắng trả hàng đã giữ; lỗi trả hàng chỉ ghi log Saga để tránh mất trạng thái đơn.
    private void safelyReleaseInventory(Order order) {
        try {
            orderSagaOrchestrator.releaseInventoryFor(order);
            sagaLogService.write(order.getId(), "INVENTORY_RELEASED", SagaLogStatus.COMPENSATED, "Inventory reservation released");
        } catch (OrderIntegrationException ignored) {
            // Đơn vẫn được đánh dấu lỗi; vận hành sau sẽ xử lý việc trả kho nếu cần.
            sagaLogService.write(order.getId(), "INVENTORY_RELEASE", SagaLogStatus.FAILED, "Could not release inventory reservation");
        }
    }

    // Hủy một đơn đang PAYMENT_PENDING và trả hàng đã giữ trong Inventory.
    private OrderResponse cancelPendingOrder(Order order, String reason) {
        if (order.getStatus() != OrderStatus.PAYMENT_PENDING) {
            throw new IllegalStateException("Only pending payment orders can be cancelled");
        }
        orderSagaOrchestrator.releaseInventoryFor(order);
        sagaLogService.write(order.getId(), "INVENTORY_RELEASED", SagaLogStatus.COMPENSATED, "Inventory released after order cancellation");
        order.markCancelled(reason);
        order = orderRepository.save(order);
        sagaLogService.write(order.getId(), "ORDER_CANCELLED", SagaLogStatus.COMPENSATED, reason);
        notificationPublisher.publish(order, "cancelled", reason);
        return OrderResponse.from(order);
    }
}
