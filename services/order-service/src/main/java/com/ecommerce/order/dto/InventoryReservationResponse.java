package com.ecommerce.order.dto;

public record InventoryReservationResponse(
        boolean reserved,
        String message
) {
}
