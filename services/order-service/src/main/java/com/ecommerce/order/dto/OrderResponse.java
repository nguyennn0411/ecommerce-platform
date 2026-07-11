package com.ecommerce.order.dto;

import com.ecommerce.order.domain.Order;
import com.ecommerce.order.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID orderId,
        UUID userId,
        String buyerName,
        String buyerEmail,
        String description,
        String currency,
        BigDecimal totalAmount,
        OrderStatus status,
        UUID paymentId,
        Long paymentOrderCode,
        String paymentLinkId,
        String checkoutUrl,
        String qrCode,
        String failureReason,
        LocalDateTime paidAt,
        LocalDateTime cancelledAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<OrderItemResponse> items
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getBuyerName(),
                order.getBuyerEmail(),
                order.getDescription(),
                order.getCurrency(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getPaymentId(),
                order.getPaymentOrderCode(),
                order.getPaymentLinkId(),
                order.getCheckoutUrl(),
                order.getQrCode(),
                order.getFailureReason(),
                order.getPaidAt(),
                order.getCancelledAt(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                order.getItems().stream().map(OrderItemResponse::from).toList()
        );
    }
}
