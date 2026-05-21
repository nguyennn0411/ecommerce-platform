package com.ecommerce.payment.application;

import org.springframework.stereotype.Service;

@Service
public class PaymentUseCase {

    public String moduleName() {
        return "Payment Service";
    }
}
