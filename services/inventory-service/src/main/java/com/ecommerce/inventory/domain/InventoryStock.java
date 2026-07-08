package com.ecommerce.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "inventory_stocks")
public class InventoryStock {

    @Id
    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "available_quantity", nullable = false)
    private int availableQuantity;

    @Column(name = "reserved_quantity", nullable = false)
    private int reservedQuantity;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected InventoryStock() {
    }

    public InventoryStock(UUID productId, String productName, int availableQuantity) {
        if (productId == null) {
            throw new IllegalArgumentException("productId is required");
        }
        if (productName == null || productName.isBlank()) {
            throw new IllegalArgumentException("productName is required");
        }
        if (availableQuantity < 0) {
            throw new IllegalArgumentException("availableQuantity must be >= 0");
        }
        this.productId = productId;
        this.productName = productName;
        this.availableQuantity = availableQuantity;
        this.reservedQuantity = 0;
    }

    public void restock(String productName, int availableQuantity) {
        if (productName == null || productName.isBlank()) {
            throw new IllegalArgumentException("productName is required");
        }
        if (availableQuantity < 0) {
            throw new IllegalArgumentException("availableQuantity must be >= 0");
        }
        this.productName = productName;
        this.availableQuantity = availableQuantity;
    }

    public void reserve(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be > 0");
        }
        if (availableQuantity < quantity) {
            throw new IllegalArgumentException("Insufficient inventory for productId=" + productId);
        }
        this.availableQuantity -= quantity;
        this.reservedQuantity += quantity;
    }

    public void release(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be > 0");
        }
        if (reservedQuantity < quantity) {
            throw new IllegalArgumentException("Reserved quantity is lower than release quantity for productId=" + productId);
        }
        this.reservedQuantity -= quantity;
        this.availableQuantity += quantity;
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public UUID getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public int getReservedQuantity() {
        return reservedQuantity;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
