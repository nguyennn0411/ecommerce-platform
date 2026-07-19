package com.ecommerce.payment.dto;

import jakarta.validation.constraints.NotBlank;

public record ConfirmPayosWebhookRequest(
        @NotBlank String webhookUrl
) {
}
