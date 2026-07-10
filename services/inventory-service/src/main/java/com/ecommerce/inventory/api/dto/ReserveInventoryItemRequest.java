package com.ecommerce.inventory.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ReserveInventoryItemRequest(
        @NotNull UUID productId,
        @NotBlank String size,
        String color,
        @NotNull @Min(1) Integer quantity
) {
}
