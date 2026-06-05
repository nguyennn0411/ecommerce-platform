package com.ecommerce.order.domain;

public enum SagaStepStatus {
    PENDING,
    RUNNING,
    SUCCESS,
    FAILED,
    COMPENSATED
}
