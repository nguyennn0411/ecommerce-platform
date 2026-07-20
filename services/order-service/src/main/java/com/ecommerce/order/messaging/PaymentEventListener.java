package com.ecommerce.order.messaging;

import com.ecommerce.common.events.PaymentCancelledEvent;
import com.ecommerce.common.events.PaymentFailedEvent;
import com.ecommerce.common.events.PaymentSuccessEvent;
import com.ecommerce.order.application.OrderService;
import com.ecommerce.order.config.RabbitMqConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class PaymentEventListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventListener.class);

    private final OrderService orderService;

    public PaymentEventListener(OrderService orderService) {
        this.orderService = orderService;
    }

    @RabbitListener(queues = RabbitMqConfig.ORDER_PAYMENT_SUCCESS_QUEUE)
    public void onPaymentSuccess(PaymentSuccessEvent event) {
        // Payment publish payment.success; Order nhan bat dong bo va xac nhan don.
        handle("payment.success", () -> orderService.handlePaymentSuccess(event.orderId()));
    }

    @RabbitListener(queues = RabbitMqConfig.ORDER_PAYMENT_FAILED_QUEUE)
    public void onPaymentFailed(PaymentFailedEvent event) {
        // Payment fail -> Service goi Inventory release va doi don sang FAILED.
        handle("payment.failed", () -> orderService.handlePaymentFailure(event.orderId(), event.reason(), false));
    }

    @RabbitListener(queues = RabbitMqConfig.ORDER_PAYMENT_CANCELLED_QUEUE)
    public void onPaymentCancelled(PaymentCancelledEvent event) {
        // Khach huy tren PayOS -> tra kho va doi don sang CANCELLED.
        handle("payment.cancelled", () -> orderService.handlePaymentFailure(event.orderId(), event.reason(), true));
    }

    private void handle(String eventName, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException exception) {
            // Log de van hanh kiem tra va xu ly lai event loi.
            log.error("Unable to process {}", eventName, exception);
        }
    }
}
