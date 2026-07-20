package com.ecommerce.order.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "buyer_name", nullable = false)
    private String buyerName;

    @Column(name = "buyer_email", nullable = false)
    private String buyerEmail;

    @Column
    private String description;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "shipping_fee", nullable = false, precision = 19, scale = 2)
    private BigDecimal shippingFee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private OrderStatus status;

    @Column(name = "payment_id")
    private UUID paymentId;

    @Column(name = "payment_order_code")
    private Long paymentOrderCode;

    @Column(name = "payment_link_id")
    private String paymentLinkId;

    @Column(name = "checkout_url")
    private String checkoutUrl;

    @Column(name = "qr_code")
    private String qrCode;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<OrderItem> items = new ArrayList<>();

    protected Order() {
    }

    public Order(UUID userId,
                 String buyerName,
                 String buyerEmail,
                 String description,
                 String currency,
                 BigDecimal shippingFee,
                 List<OrderItem> items) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item");
        }
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.buyerName = buyerName;
        this.buyerEmail = buyerEmail;
        this.description = description;
        this.currency = currency;
        this.shippingFee = shippingFee == null ? BigDecimal.ZERO : shippingFee;
        // Đơn mới tạo, chưa giữ hàng/chưa tạo Payment.
        this.status = OrderStatus.CREATED;
        // Tổng tiền = tiền các item + phí giao hàng.
        this.totalAmount = items.stream()
                .map(OrderItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .add(this.shippingFee);
        items.forEach(this::addItem);
    }

    private void addItem(OrderItem item) {
        item.attachTo(this);
        this.items.add(item);
    }

    public void markPaymentPending(UUID paymentId, Long paymentOrderCode, String paymentLinkId, String checkoutUrl, String qrCode) {
        this.paymentId = paymentId;
        this.paymentOrderCode = paymentOrderCode;
        this.paymentLinkId = paymentLinkId;
        this.checkoutUrl = checkoutUrl;   // FE dùng link này mở PayOS.
        this.qrCode = qrCode;
        this.failureReason = null;
        this.cancelledAt = null;
        this.status = OrderStatus.PAYMENT_PENDING;
    }

    // Payment thành công và Inventory đã trừ kho
    public void markConfirmed() {
        this.status = OrderStatus.CONFIRMED;
        this.failureReason = null;
        this.cancelledAt = null;
        this.paidAt = LocalDateTime.now();
    }

    // Payment lỗi hoặc người dùng không trả tiền kịp.
    public void markFailed(String reason) {
        this.status = OrderStatus.FAILED;
        this.failureReason = reason;
    }

    public void markCancelled(String reason) {
        this.status = OrderStatus.CANCELLED;
        this.failureReason = reason;
        this.cancelledAt = LocalDateTime.now();
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

    public UUID getId() {
        return id;
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

    public String getDescription() {
        return description;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public BigDecimal getShippingFee() {
        return shippingFee;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public Long getPaymentOrderCode() {
        return paymentOrderCode;
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

    public List<OrderItem> getItems() {
        return List.copyOf(items);
    }
}
