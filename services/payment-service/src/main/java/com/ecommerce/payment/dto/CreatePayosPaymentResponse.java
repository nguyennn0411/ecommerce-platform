package com.ecommerce.payment.dto;

import com.ecommerce.payment.enums.PaymentMethod;
import com.ecommerce.payment.enums.PaymentProvider;
import com.ecommerce.payment.enums.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CreatePayosPaymentResponse(
        UUID paymentId,
        UUID orderId,
        Long orderCode,
        String buyerName,
        String buyerEmail,
        PaymentStatus status,
        BigDecimal amount,
        String currency,
        PaymentMethod paymentMethod,
        PaymentProvider paymentProvider,
        String paymentLinkId,
        String checkoutUrl,
        String qrCode,
        String message
) {
}
