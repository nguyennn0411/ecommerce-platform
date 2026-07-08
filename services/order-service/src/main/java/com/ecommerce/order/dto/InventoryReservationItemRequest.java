package com.ecommerce.order.dto;

import java.util.UUID;

public record InventoryReservationItemRequest(
        UUID productId,
        String productName,
        int quantity
) {
}
