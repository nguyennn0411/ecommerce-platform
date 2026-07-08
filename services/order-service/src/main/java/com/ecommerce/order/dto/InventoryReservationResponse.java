package com.ecommerce.order.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record InventoryReservationResponse(
        UUID orderId,
        boolean reserved,
        boolean released,
        String message,
        List<Map<String, Object>> items
) {
}
