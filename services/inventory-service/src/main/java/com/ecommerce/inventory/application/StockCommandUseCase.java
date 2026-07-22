package com.ecommerce.inventory.application;

import com.ecommerce.inventory.api.dto.BulkStockItemRequest;
import com.ecommerce.inventory.api.dto.BulkUpsertStockRequest;
import com.ecommerce.inventory.api.dto.ProductVariantStocksResponse;
import com.ecommerce.inventory.api.dto.UpsertStockRequest;
import com.ecommerce.inventory.api.dto.VariantStockResponse;
import com.ecommerce.inventory.domain.InventoryItem;
import com.ecommerce.inventory.persistence.InventoryItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class StockCommandUseCase {

    private final InventoryItemRepository inventoryItemRepository;

    public StockCommandUseCase(InventoryItemRepository inventoryItemRepository) {
        this.inventoryItemRepository = inventoryItemRepository;
    }

    @Transactional
    public VariantStockResponse upsert(UpsertStockRequest request) {
        return upsertVariant(
                request.productId(),
                request.size(),
                request.color(),
                request.quantity()
        );
    }

    /**
     * Upsert all variants for one product. Any failure rolls back the whole batch.
     */
    @Transactional
    public ProductVariantStocksResponse upsertBulk(BulkUpsertStockRequest request) {
        UUID productId = request.productId();
        List<VariantStockResponse> results = new ArrayList<>();

        for (BulkStockItemRequest item : request.items()) {
            results.add(upsertVariant(productId, item.size(), item.color(), item.quantity()));
        }

        return new ProductVariantStocksResponse(productId, List.copyOf(results));
    }

    private VariantStockResponse upsertVariant(UUID productId, String size, String color, int quantity) {
        String normalizedSize = normalizeSize(size);
        String normalizedColor = normalizeColor(color);

        InventoryItem item = inventoryItemRepository
                .findByProductIdAndSizeAndColor(productId, normalizedSize, normalizedColor)
                .map(existing -> {
                    existing.setAbsoluteQuantity(quantity);
                    return existing;
                })
                .orElseGet(() -> InventoryItem.createNew(productId, normalizedSize, normalizedColor, quantity));

        return VariantStockResponse.from(inventoryItemRepository.save(item));
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
