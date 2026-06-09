package com.ecommerce.payment.exception;

import java.util.UUID;

public class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException(String message) {
        super(message);
    }

    public static PaymentNotFoundException byOrderId(UUID orderId) {
        return new PaymentNotFoundException("Payment not found for orderId=%s".formatted(orderId));
    }

    public static PaymentNotFoundException byOrderCode(Long orderCode) {
        return new PaymentNotFoundException("Payment not found for orderCode=%s".formatted(orderCode));
    }

    public static PaymentNotFoundException byPaymentId(UUID paymentId) {
        return new PaymentNotFoundException("Payment not found for paymentId=%s".formatted(paymentId));
    }
}
