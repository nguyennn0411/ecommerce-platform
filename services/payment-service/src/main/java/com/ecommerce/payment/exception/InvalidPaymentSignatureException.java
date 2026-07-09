package com.ecommerce.payment.exception;

public class InvalidPaymentSignatureException extends RuntimeException {

    public InvalidPaymentSignatureException() {
        super("Invalid PayOS webhook signature");
    }
}
