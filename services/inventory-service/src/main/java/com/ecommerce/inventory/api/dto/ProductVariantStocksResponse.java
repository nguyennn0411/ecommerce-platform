package com.ecommerce.inventory.api.dto;

import java.util.List;
import java.util.UUID;

public record ProductVariantStocksResponse(
        UUID productId,
        List<VariantStockResponse> variants
) {
}
