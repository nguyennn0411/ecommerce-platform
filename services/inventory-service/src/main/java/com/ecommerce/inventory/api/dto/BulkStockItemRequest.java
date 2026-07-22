package com.ecommerce.inventory.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BulkStockItemRequest(
        @NotBlank String size,
        String color,
        @NotNull @Min(0) Integer quantity
) {
}
