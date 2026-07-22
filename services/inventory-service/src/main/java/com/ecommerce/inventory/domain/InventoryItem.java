package com.ecommerce.inventory.domain;

import com.ecommerce.inventory.domain.vo.Quantity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "inventory_items")
public class InventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(nullable = false, length = 20)
    private String size;

    @Column(length = 50)
    private String color;

    @Column(nullable = false)
    private int quantity = 0;

    @Column(name = "reserved_quantity", nullable = false)
    private int reservedQuantity = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private InventoryItemStatus status = InventoryItemStatus.IN_STOCK;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public int getAvailableQuantity() {
        return quantity - reservedQuantity;
    }

    public void reserve(Quantity requested) {
        int amount = requested.value();
        if (amount <= 0 || getAvailableQuantity() < amount) {
            throw new IllegalStateException("Not enough stock");
        }
        reservedQuantity += amount;
    }

    public void confirm(Quantity confirmed) {
        int amount = confirmed.value();
        if (amount <= 0 || reservedQuantity < amount) {
            throw new IllegalStateException("Invalid confirm amount");
        }
        reservedQuantity -= amount;
        quantity -= amount;
    }

    public void release(Quantity released) {
        int amount = released.value();
        if (amount <= 0 || reservedQuantity < amount) {
            throw new IllegalStateException("Invalid release amount");
        }
        reservedQuantity -= amount;
    }

    /**
     * Admin/catalog set stock to an absolute quantity (not incremental).
     * Must not go below reservedQuantity (DB check + open reservations).
     */
    public void setAbsoluteQuantity(int newQuantity) {
        if (newQuantity < 0) {
            throw new IllegalArgumentException("quantity must be >= 0");
        }
        if (newQuantity < reservedQuantity) {
            throw new IllegalArgumentException(
                    "Cannot set quantity=%d because reservedQuantity=%d"
                            .formatted(newQuantity, reservedQuantity)
            );
        }
        this.quantity = newQuantity;
        this.status = getAvailableQuantity() > 0
                ? InventoryItemStatus.IN_STOCK
                : InventoryItemStatus.OUT_OF_STOCK;
    }

    public static InventoryItem createNew(UUID productId, String size, String color, int quantity) {
        InventoryItem item = new InventoryItem();
        item.productId = productId;
        item.size = size;
        item.color = color;
        item.reservedQuantity = 0;
        item.setAbsoluteQuantity(quantity);
        return item;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProductId() {
        return productId;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getReservedQuantity() {
        return reservedQuantity;
    }

    public InventoryItemStatus getStatus() {
        return status;
    }

    public void setStatus(InventoryItemStatus status) {
        this.status = status;
    }
}
