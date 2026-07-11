package com.ecommerce.inventory.application;

import com.ecommerce.inventory.api.dto.ProductVariantStocksResponse;
import com.ecommerce.inventory.api.dto.VariantStockResponse;
import com.ecommerce.inventory.persistence.InventoryItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class StockQueryUseCase {

    private final InventoryItemRepository inventoryItemRepository;

    public StockQueryUseCase(InventoryItemRepository inventoryItemRepository) {
        this.inventoryItemRepository = inventoryItemRepository;
    }

    @Transactional(readOnly = true)
    public ProductVariantStocksResponse listVariantStocks(UUID productId) {
        List<VariantStockResponse> variants = inventoryItemRepository.findByProductId(productId).stream()
                .map(VariantStockResponse::from)
                .toList();
        return new ProductVariantStocksResponse(productId, variants);
    }

    @Transactional(readOnly = true)
    public VariantStockResponse getVariantStock(UUID productId, String size, String color) {
        String normalizedSize = normalizeSize(size);
        String normalizedColor = normalizeColor(color);

        return inventoryItemRepository
                .findByProductIdAndSizeAndColor(productId, normalizedSize, normalizedColor)
                .map(VariantStockResponse::from)
                .orElseGet(() -> VariantStockResponse.unavailable(productId, normalizedSize, normalizedColor));
    }

    private String normalizeSize(String size) {
        if (size == null || size.isBlank()) {
            throw new IllegalArgumentException("size is required");
        }
        return size.trim();
    }

    private String normalizeColor(String color) {
        if (color == null) {
            return null;
        }
        String trimmed = color.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
