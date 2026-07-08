package com.ecommerce.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record InventoryItemRequest(
        @NotNull UUID productId,
        String productName,
        @Min(1) int quantity
) {
}
