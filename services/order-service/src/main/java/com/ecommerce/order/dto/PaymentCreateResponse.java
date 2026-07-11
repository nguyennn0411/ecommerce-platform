package com.ecommerce.order.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentCreateResponse(
        UUID paymentId,
        UUID orderId,
        Long orderCode,
        String buyerName,
        String buyerEmail,
        String status,
        BigDecimal amount,
        String currency,
        String paymentMethod,
        String paymentProvider,
        String paymentLinkId,
        String checkoutUrl,
        String qrCode,
        String message
) {
}
