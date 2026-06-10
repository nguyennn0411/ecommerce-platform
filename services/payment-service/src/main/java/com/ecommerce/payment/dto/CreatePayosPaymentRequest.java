package com.ecommerce.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreatePayosPaymentRequest(
        @NotNull UUID orderId,
        @NotNull UUID userId,
        @NotNull @DecimalMin(value = "1000", message = "Amount must be at least 1000 VND") BigDecimal amount,
        String currency,
        @NotBlank String description,
        @NotBlank String buyerName,
        @NotBlank @Email String buyerEmail,
        String buyerPhone,
        String buyerAddress
) {
}
