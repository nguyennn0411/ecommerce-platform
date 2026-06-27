package com.ecommerce.inventory.api.dto;

public record InventoryReservationResponse(
        boolean reserved,
        String message
) {
}
