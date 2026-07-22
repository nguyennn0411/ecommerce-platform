package com.ecommerce.order.messaging;

import java.time.Instant;
import java.util.UUID;

/** Contract Order phát cho Notification Service qua RabbitMQ. */
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
