package com.ecommerce.payment.exception;

import java.util.UUID;

public class PaymentAlreadyProcessedException extends RuntimeException {

    public PaymentAlreadyProcessedException(UUID orderId) {
        super("Payment already processed for orderId=%s".formatted(orderId));
    }
}
