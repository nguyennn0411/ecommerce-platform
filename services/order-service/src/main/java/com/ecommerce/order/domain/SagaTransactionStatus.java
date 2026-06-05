package com.ecommerce.order.domain;

public enum SagaTransactionStatus {
    STARTED,
    COMPLETED,
    FAILED,
    COMPENSATING,
    COMPENSATED
}
