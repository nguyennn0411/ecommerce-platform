package com.ecommerce.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record InventoryStockUpsertRequest(
        @NotNull UUID productId,
        @NotBlank String productName,
        @Min(0) int availableQuantity
) {
}
