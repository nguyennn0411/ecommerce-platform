package com.ecommerce.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.PrePersist;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "saga_transaction_logs")
public class SagaTransactionLog {

    @Id
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(nullable = false, length = 80)
    private String step;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SagaLogStatus status;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected SagaTransactionLog() {
    }

    public SagaTransactionLog(UUID orderId, String step, SagaLogStatus status, String message) {
        this.id = UUID.randomUUID();
        this.orderId = orderId;
        this.step = step;
        this.status = status;
        this.message = message;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getOrderId() { return orderId; }
    public String getStep() { return step; }
    public SagaLogStatus getStatus() { return status; }
    public String getMessage() { return message; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
