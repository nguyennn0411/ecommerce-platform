package com.ecommerce.order.application;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PaymentTimeoutScheduler {

    private final OrderService orderService;

    public PaymentTimeoutScheduler(OrderService orderService) {
        this.orderService = orderService;
    }

    @Scheduled(fixedDelayString = "${order.payment-timeout.check-interval-ms:30000}")
    public void cancelExpiredPayments() {
        orderService.cancelExpiredPaymentOrders();
    }
}
