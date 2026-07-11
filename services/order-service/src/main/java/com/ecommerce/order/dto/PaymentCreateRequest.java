package com.ecommerce.order.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentCreateRequest(
        UUID orderId,
        UUID userId,
        BigDecimal amount,
        String currency,
        String description,
        String buyerName,
        String buyerEmail,
        String buyerPhone,
        String buyerAddress
) {
}
