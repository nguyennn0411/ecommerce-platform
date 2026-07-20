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
        // Chay moi 30 giay de huy don pending qua han va giai phong hang da giu.
        orderService.cancelExpiredPaymentOrders();
    }
}
