package com.ecommerce.order.integration;

public record InventoryReservationResponse(
        boolean reserved,
        String message
) {
}
