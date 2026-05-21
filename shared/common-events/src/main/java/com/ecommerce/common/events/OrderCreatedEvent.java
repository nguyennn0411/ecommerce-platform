package com.ecommerce.common.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderCreatedEvent(
        UUID orderId,
        UUID userId,
        BigDecimal totalAmount,
        Instant createdAt
) {
}
