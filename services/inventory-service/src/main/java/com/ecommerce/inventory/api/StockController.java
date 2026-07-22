package com.ecommerce.inventory.api;

import com.ecommerce.common.web.ApiResponse;
import com.ecommerce.inventory.api.dto.BulkUpsertStockRequest;
import com.ecommerce.inventory.api.dto.ProductVariantStocksResponse;
import com.ecommerce.inventory.api.dto.UpsertStockRequest;
import com.ecommerce.inventory.api.dto.VariantStockResponse;
import com.ecommerce.inventory.application.StockCommandUseCase;
import com.ecommerce.inventory.application.StockQueryUseCase;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventory/stocks")
public class StockController {

    private final StockQueryUseCase stockQueryUseCase;
    private final StockCommandUseCase stockCommandUseCase;

    public StockController(StockQueryUseCase stockQueryUseCase, StockCommandUseCase stockCommandUseCase) {
        this.stockQueryUseCase = stockQueryUseCase;
        this.stockCommandUseCase = stockCommandUseCase;
    }

    /**
     * Load all variant stocks for a product — frontend picks size/color then looks up locally.
     */
    @GetMapping("/{productId}/variants")
    public ApiResponse<ProductVariantStocksResponse> listVariantStocks(
            @PathVariable("productId") UUID productId
    ) {
        return ApiResponse.ok(stockQueryUseCase.listVariantStocks(productId));
    }

    /**
     * Stock for one variant (productId + size + color). Returns availableQuantity = 0 when not found.
     */
    @GetMapping
    public ApiResponse<VariantStockResponse> getVariantStock(
            @RequestParam("productId") UUID productId,
            @RequestParam("size") String size,
            @RequestParam(value = "color", required = false) String color
    ) {
        return ApiResponse.ok(stockQueryUseCase.getVariantStock(productId, size, color));
    }

    /**
     * Admin/catalog: create or set absolute quantity for one variant.
     */
    @PutMapping
    public ApiResponse<VariantStockResponse> upsertStock(@Valid @RequestBody UpsertStockRequest request) {
        return ApiResponse.ok(stockCommandUseCase.upsert(request), "Stock saved");
    }

    /**
     * Admin/catalog: set stock for many size/color variants of one product (all-or-nothing).
     */
    @PutMapping("/bulk")
    public ApiResponse<ProductVariantStocksResponse> upsertBulkStock(
            @Valid @RequestBody BulkUpsertStockRequest request
    ) {
        return ApiResponse.ok(stockCommandUseCase.upsertBulk(request), "Stock variants saved");
    }
}
