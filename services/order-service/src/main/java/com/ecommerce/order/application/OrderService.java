package com.ecommerce.order.application;

import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.dto.SagaTransactionLogResponse;
import com.ecommerce.order.domain.OrderStatus;

import java.util.List;
import java.util.UUID;

public interface OrderService {

    OrderResponse createOrder(CreateOrderRequest request);

    OrderResponse getOrder(UUID orderId);

    List<OrderResponse> getOrdersByUser(UUID userId);

    List<OrderResponse> getAllOrders(OrderStatus status);

    List<SagaTransactionLogResponse> getSagaLogs(UUID orderId);

    OrderResponse cancelOrder(UUID orderId, UUID userId, String reason);

    OrderResponse cancelOrderByStaff(UUID orderId, String reason);

    OrderResponse completeOrderByStaff(UUID orderId);

    OrderResponse markShippingByStaff(UUID orderId);

    OrderResponse returnOrderByStaff(UUID orderId, String reason);

    void handlePaymentSuccess(UUID orderId);

    void handlePaymentFailure(UUID orderId, String reason, boolean cancelled);

    void cancelExpiredPaymentOrders();
}
