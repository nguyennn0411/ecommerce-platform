package com.ecommerce.order.api.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record CompletePaymentRequest(
        UUID paymentId,
        @NotBlank String paymentStatus,
        String transactionId
) {
}
