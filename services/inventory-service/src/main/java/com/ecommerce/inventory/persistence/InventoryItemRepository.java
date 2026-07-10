package com.ecommerce.inventory.persistence;

import com.ecommerce.inventory.domain.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID> {

    Optional<InventoryItem> findFirstByProductId(UUID productId);

    List<InventoryItem> findByProductId(UUID productId);

    Optional<InventoryItem> findByProductIdAndSizeAndColor(UUID productId, String size, String color);
}
