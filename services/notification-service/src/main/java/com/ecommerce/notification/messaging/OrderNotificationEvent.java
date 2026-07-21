package com.ecommerce.notification.messaging;

import java.time.Instant;
import java.util.UUID;

public record OrderNotificationEvent(
        UUID orderId,
        UUID userId,
        String recipientEmail,
        String eventType,
        String orderStatus,
        String message,
        Instant occurredAt
) {
}
