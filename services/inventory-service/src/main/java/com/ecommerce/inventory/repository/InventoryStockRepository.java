package com.ecommerce.inventory.repository;

import com.ecommerce.inventory.domain.InventoryStock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InventoryStockRepository extends JpaRepository<InventoryStock, UUID> {
}
