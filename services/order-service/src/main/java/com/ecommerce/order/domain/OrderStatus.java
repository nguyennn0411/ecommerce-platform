package com.ecommerce.order.domain;

public enum OrderStatus {
    PENDING,
    PENDING_PAYMENT,
    CONFIRMED,
    PAID,
    SHIPPING,
    COMPLETED,
    CANCELLED,
    FAILED
}
