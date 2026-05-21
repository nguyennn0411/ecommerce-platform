package com.ecommerce.order.application;

import org.springframework.stereotype.Service;

@Service
public class OrderSagaOrchestrator {

    public String createOrderSagaFlow() {
        return "validate-user -> validate-products -> reserve-inventory -> create-order -> process-payment -> send-notification";
    }
}
