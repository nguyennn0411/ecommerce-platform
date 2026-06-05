package com.ecommerce.order.api.dto;

import com.ecommerce.order.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String orderCode,
        UUID userId,
        BigDecimal totalAmount,
        String currency,
        OrderStatus status,
        String shippingAddress,
        UUID paymentId,
        String note,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<OrderItemResponse> items
) {
}
