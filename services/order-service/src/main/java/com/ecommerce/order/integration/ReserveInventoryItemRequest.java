package com.ecommerce.order.integration;

import java.util.UUID;

public record ReserveInventoryItemRequest(
        UUID productId,
        Integer quantity
) {
}
