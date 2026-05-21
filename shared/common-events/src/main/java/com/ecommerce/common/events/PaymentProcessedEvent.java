package com.ecommerce.common.events;

import java.time.Instant;
import java.util.UUID;

public record PaymentProcessedEvent(
        UUID orderId,
        String paymentStatus,
        String transactionId,
        Instant occurredAt
) {
}
