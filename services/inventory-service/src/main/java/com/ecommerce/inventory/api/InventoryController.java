package com.ecommerce.inventory.api;

import com.ecommerce.common.web.ApiResponse;
import com.ecommerce.inventory.application.InventoryService;
import com.ecommerce.inventory.dto.InventoryReservationRequest;
import com.ecommerce.inventory.dto.InventoryReservationResponse;
import com.ecommerce.inventory.dto.InventoryStockResponse;
import com.ecommerce.inventory.dto.InventoryStockUpsertRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping("/stocks")
    public ApiResponse<InventoryStockResponse> upsertStock(@Valid @RequestBody InventoryStockUpsertRequest request) {
        return ok(inventoryService.upsertStock(request), "Inventory stock saved");
    }

    @GetMapping("/stocks/{productId}")
    public ApiResponse<InventoryStockResponse> getStock(@PathVariable UUID productId) {
        return ok(inventoryService.getStock(productId), "OK");
    }

    @PostMapping("/reservations")
    public ApiResponse<InventoryReservationResponse> reserve(@Valid @RequestBody InventoryReservationRequest request) {
        return ok(inventoryService.reserve(request), "Inventory reserved");
    }

    @PostMapping("/reservations/release")
    public ApiResponse<InventoryReservationResponse> release(@Valid @RequestBody InventoryReservationRequest request) {
        return ok(inventoryService.release(request), "Inventory released");
    }

    private <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, data, message, Instant.now());
    }
}
