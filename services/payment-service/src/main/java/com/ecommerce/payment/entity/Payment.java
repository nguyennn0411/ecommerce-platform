package com.ecommerce.payment.entity;

import com.ecommerce.payment.enums.PaymentMethod;
import com.ecommerce.payment.enums.PaymentProvider;
import com.ecommerce.payment.enums.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class Payment implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    @Column(name = "order_code", nullable = false, unique = true)
    private Long orderCode;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "buyer_name", length = 255)
    private String buyerName;

    @Column(name = "buyer_email", length = 255)
    private String buyerEmail;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 50)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_provider", nullable = false, length = 50)
    private PaymentProvider paymentProvider;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private PaymentStatus status;

    @Column(name = "payment_link_id")
    private String paymentLinkId;

    @Column(name = "checkout_url", columnDefinition = "TEXT")
    private String checkoutUrl;

    @Column(name = "qr_code", columnDefinition = "TEXT")
    private String qrCode;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Transient
    private boolean isNew = true;

    protected Payment() {
    }

    public Payment(UUID orderId,
                   UUID userId,
                   Long orderCode,
                   BigDecimal amount,
                   String currency,
                   String buyerName,
                   String buyerEmail) {
        this.id = UUID.randomUUID();
        this.orderId = orderId;
        this.userId = userId;
        this.orderCode = orderCode;
        this.amount = amount;
        this.currency = currency;
        this.buyerName = buyerName;
        this.buyerEmail = buyerEmail;
        this.paymentMethod = PaymentMethod.PAYOS;
        this.paymentProvider = PaymentProvider.PAYOS;
        this.status = PaymentStatus.PENDING;
    }

    public void attachCheckoutInfo(String paymentLinkId, String checkoutUrl, String qrCode) {
        this.paymentLinkId = paymentLinkId;
        this.checkoutUrl = checkoutUrl;
        this.qrCode = qrCode;
        this.failureReason = null;
    }

    public void markSuccess() {
        if (status == PaymentStatus.SUCCESS) {
            return;
        }
        status = PaymentStatus.SUCCESS;
        failureReason = null;
        paidAt = LocalDateTime.now();
        cancelledAt = null;
    }

    public void markFailed(String reason) {
        if (status == PaymentStatus.SUCCESS || status == PaymentStatus.REFUNDED) {
            return;
        }
        status = PaymentStatus.FAILED;
        failureReason = reason;
    }

    public void markCancelled(String reason) {
        if (status == PaymentStatus.SUCCESS || status == PaymentStatus.REFUNDED) {
            return;
        }
        status = PaymentStatus.CANCELLED;
        failureReason = reason;
        cancelledAt = LocalDateTime.now();
    }

    public void markRefunded() {
        status = PaymentStatus.REFUNDED;
        failureReason = null;
    }

    public boolean isFinalized() {
        return status == PaymentStatus.SUCCESS
                || status == PaymentStatus.FAILED
                || status == PaymentStatus.CANCELLED
                || status == PaymentStatus.REFUNDED;
    }

    public boolean isPending() {
        return status == PaymentStatus.PENDING;
    }

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (status == null) {
            status = PaymentStatus.PENDING;
        }
        if (paymentMethod == null) {
            paymentMethod = PaymentMethod.PAYOS;
        }
        if (paymentProvider == null) {
            paymentProvider = PaymentProvider.PAYOS;
        }
        if (currency != null) {
            currency = currency.toUpperCase();
        }
        if (buyerName != null) {
            buyerName = buyerName.trim();
        }
        if (buyerEmail != null) {
            buyerEmail = buyerEmail.trim().toLowerCase();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        isNew = false;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public Long getOrderCode() {
        return orderCode;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getBuyerName() {
        return buyerName;
    }

    public String getBuyerEmail() {
        return buyerEmail;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public PaymentProvider getPaymentProvider() {
        return paymentProvider;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getPaymentLinkId() {
        return paymentLinkId;
    }

    public String getCheckoutUrl() {
        return checkoutUrl;
    }

    public String getQrCode() {
        return qrCode;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
