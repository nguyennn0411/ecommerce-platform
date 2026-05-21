package com.ecommerce.common.events;

import java.time.Instant;
import java.util.UUID;

public record InventoryReservedEvent(
        UUID orderId,
        boolean reserved,
        String reason,
        Instant occurredAt
) {
}
