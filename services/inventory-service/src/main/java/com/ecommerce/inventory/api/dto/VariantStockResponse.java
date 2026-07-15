package com.ecommerce.inventory.api.dto;

import com.ecommerce.inventory.domain.InventoryItem;
import com.ecommerce.inventory.domain.InventoryItemStatus;

import java.util.UUID;

public record VariantStockResponse(
        UUID productId,
        String size,
        String color,
        int quantity,
        int reservedQuantity,
        int availableQuantity,
        String status,
        boolean inStock
) {
    public static VariantStockResponse from(InventoryItem item) {
        int available = item.getAvailableQuantity();
        boolean sellable = item.getStatus() == InventoryItemStatus.IN_STOCK && available > 0;
        return new VariantStockResponse(
                item.getProductId(),
                item.getSize(),
                item.getColor(),
                item.getQuantity(),
                item.getReservedQuantity(),
                available,
                item.getStatus().name(),
                sellable
        );
    }

    public static VariantStockResponse unavailable(UUID productId, String size, String color) {
        return new VariantStockResponse(
                productId,
                size,
                color,
                0,
                0,
                0,
                InventoryItemStatus.OUT_OF_STOCK.name(),
                false
        );
    }
}
