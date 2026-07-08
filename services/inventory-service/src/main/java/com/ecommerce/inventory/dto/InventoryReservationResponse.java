package com.ecommerce.inventory.dto;

import java.util.List;
import java.util.UUID;

public record InventoryReservationResponse(
        UUID orderId,
        boolean reserved,
        boolean released,
        String message,
        List<InventoryStockResponse> items
) {
}
