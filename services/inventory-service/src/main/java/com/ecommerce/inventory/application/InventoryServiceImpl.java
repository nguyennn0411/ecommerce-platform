package com.ecommerce.inventory.application;

import com.ecommerce.inventory.domain.InventoryStock;
import com.ecommerce.inventory.dto.InventoryItemRequest;
import com.ecommerce.inventory.dto.InventoryReservationRequest;
import com.ecommerce.inventory.dto.InventoryReservationResponse;
import com.ecommerce.inventory.dto.InventoryStockResponse;
import com.ecommerce.inventory.dto.InventoryStockUpsertRequest;
import com.ecommerce.inventory.exception.InventoryNotFoundException;
import com.ecommerce.inventory.repository.InventoryStockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class InventoryServiceImpl implements InventoryService {

    private final InventoryStockRepository inventoryStockRepository;

    public InventoryServiceImpl(InventoryStockRepository inventoryStockRepository) {
        this.inventoryStockRepository = inventoryStockRepository;
    }

    @Override
    @Transactional
    public InventoryStockResponse upsertStock(InventoryStockUpsertRequest request) {
        InventoryStock stock = inventoryStockRepository.findById(request.productId())
                .map(existing -> {
                    existing.restock(request.productName(), request.availableQuantity());
                    return existing;
                })
                .orElseGet(() -> new InventoryStock(request.productId(), request.productName(), request.availableQuantity()));
        return InventoryStockResponse.from(inventoryStockRepository.save(stock));
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryStockResponse getStock(UUID productId) {
        return InventoryStockResponse.from(findStock(productId));
    }

    @Override
    @Transactional
    public InventoryReservationResponse reserve(InventoryReservationRequest request) {
        List<InventoryStock> stocks = request.items().stream()
                .map(this::findStockFromItem)
                .toList();
        for (int i = 0; i < stocks.size(); i++) {
            stocks.get(i).reserve(request.items().get(i).quantity());
        }
        return new InventoryReservationResponse(
                request.orderId(),
                true,
                false,
                "Inventory reserved successfully",
                inventoryStockRepository.saveAll(stocks).stream().map(InventoryStockResponse::from).toList()
        );
    }

    @Override
    @Transactional
    public InventoryReservationResponse release(InventoryReservationRequest request) {
        List<InventoryStock> stocks = request.items().stream()
                .map(this::findStockFromItem)
                .toList();
        for (int i = 0; i < stocks.size(); i++) {
            stocks.get(i).release(request.items().get(i).quantity());
        }
        return new InventoryReservationResponse(
                request.orderId(),
                false,
                true,
                "Inventory released successfully",
                inventoryStockRepository.saveAll(stocks).stream().map(InventoryStockResponse::from).toList()
        );
    }

    private InventoryStock findStockFromItem(InventoryItemRequest item) {
        return findStock(item.productId());
    }

    private InventoryStock findStock(UUID productId) {
        return inventoryStockRepository.findById(productId).orElseThrow(() -> new InventoryNotFoundException(productId));
    }
}
