package com.ecommerce.payment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PayosCreatePaymentRequest(
        Long orderCode,
        Long amount,
        String description,
        String buyerName,
        String buyerEmail,
        String buyerPhone,
        String buyerAddress,
        String cancelUrl,
        String returnUrl,
        String signature
) {
}
