package com.ecommerce.payment.dto;

import jakarta.validation.constraints.Size;

public record CancelPaymentRequest(
        @Size(max = 255) String reason
) {
}
