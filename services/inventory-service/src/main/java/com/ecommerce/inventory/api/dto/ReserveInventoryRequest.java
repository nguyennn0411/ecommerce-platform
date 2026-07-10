package com.ecommerce.inventory.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record ReserveInventoryRequest(
        @NotNull UUID orderId,
        @NotEmpty @Valid List<ReserveInventoryItemRequest> items
) {
}
