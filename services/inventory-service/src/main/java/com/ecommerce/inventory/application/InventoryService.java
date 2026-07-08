package com.ecommerce.inventory.application;

import com.ecommerce.inventory.dto.InventoryReservationRequest;
import com.ecommerce.inventory.dto.InventoryReservationResponse;
import com.ecommerce.inventory.dto.InventoryStockResponse;
import com.ecommerce.inventory.dto.InventoryStockUpsertRequest;

import java.util.UUID;

public interface InventoryService {

    InventoryStockResponse upsertStock(InventoryStockUpsertRequest request);

    InventoryStockResponse getStock(UUID productId);

    InventoryReservationResponse reserve(InventoryReservationRequest request);

    InventoryReservationResponse release(InventoryReservationRequest request);
}
