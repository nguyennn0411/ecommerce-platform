package com.ecommerce.order.domain;

public enum OrderStatus {
    CREATED,
    PAYMENT_PENDING,
    CONFIRMED,
    SHIPPING,
    COMPLETED,
    RETURNED,
    FAILED,
    CANCELLED
}
