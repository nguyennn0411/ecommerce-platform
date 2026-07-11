package com.ecommerce.order.application;

import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.dto.OrderResponse;

import java.util.List;
import java.util.UUID;

public interface OrderService {

    OrderResponse createOrder(CreateOrderRequest request);

    OrderResponse getOrder(UUID orderId);

    List<OrderResponse> getOrdersByUser(UUID userId);

    void handlePaymentSuccess(UUID orderId);

    void handlePaymentFailure(UUID orderId, String reason, boolean cancelled);
}
