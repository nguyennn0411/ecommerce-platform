package com.ecommerce.payment.dto;

import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.enums.PaymentMethod;
import com.ecommerce.payment.enums.PaymentProvider;
import com.ecommerce.payment.enums.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PaymentResponse(
        UUID paymentId,
        UUID orderId,
        Long orderCode,
        UUID userId,
        String buyerName,
        String buyerEmail,
        BigDecimal amount,
        String currency,
        PaymentMethod paymentMethod,
        PaymentProvider paymentProvider,
        PaymentStatus status,
        String paymentLinkId,
        String checkoutUrl,
        String qrCode,
        String failureReason,
        LocalDateTime paidAt,
        LocalDateTime cancelledAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getOrderCode(),
                payment.getUserId(),
                payment.getBuyerName(),
                payment.getBuyerEmail(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getPaymentMethod(),
                payment.getPaymentProvider(),
                payment.getStatus(),
                payment.getPaymentLinkId(),
                payment.getCheckoutUrl(),
                payment.getQrCode(),
                payment.getFailureReason(),
                payment.getPaidAt(),
                payment.getCancelledAt(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}
