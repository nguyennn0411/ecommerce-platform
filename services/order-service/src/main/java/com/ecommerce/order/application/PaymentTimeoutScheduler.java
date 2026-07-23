package com.ecommerce.order.application;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PaymentTimeoutScheduler {

    private final OrderService orderService;

    public PaymentTimeoutScheduler(OrderService orderService) {
        this.orderService = orderService;
    }

    // Tự động quét đơn chờ thanh toán quá hạn theo chu kỳ cấu hình trong application.yml.
    @Scheduled(fixedDelayString = "${order.payment-timeout.check-interval-ms:30000}")
    public void cancelExpiredPayments() {
        // Chạy mỗi 30 giây để hủy đơn pending quá hạn và giải phóng hàng đã giữ.
        orderService.cancelExpiredPaymentOrders();
    }
}
