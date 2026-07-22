package com.ecommerce.payment.dto;

public record ConfirmPayosWebhookResponse(
        String webhookUrl,
        String channelName,
        String shortName
) {
}
