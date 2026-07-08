package com.ecommerce.inventory.dto;

import com.ecommerce.inventory.domain.InventoryStock;

import java.time.LocalDateTime;
import java.util.UUID;

public record InventoryStockResponse(
        UUID productId,
        String productName,
        int availableQuantity,
        int reservedQuantity,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static InventoryStockResponse from(InventoryStock stock) {
        return new InventoryStockResponse(
                stock.getProductId(),
                stock.getProductName(),
                stock.getAvailableQuantity(),
                stock.getReservedQuantity(),
                stock.getCreatedAt(),
                stock.getUpdatedAt()
        );
    }
}
