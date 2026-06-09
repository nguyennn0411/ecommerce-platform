package com.ecommerce.notification.messaging;

import com.ecommerce.common.events.PaymentCancelledEvent;
import com.ecommerce.common.events.PaymentCreatedEvent;
import com.ecommerce.common.events.PaymentFailedEvent;
import com.ecommerce.common.events.PaymentRefundedEvent;
import com.ecommerce.common.events.PaymentSuccessEvent;
import com.ecommerce.notification.config.RabbitMqConfig;
import com.ecommerce.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class PaymentNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentNotificationListener.class);

    private final NotificationService notificationService;

    public PaymentNotificationListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = RabbitMqConfig.PAYMENT_CREATED_QUEUE)
    public void onPaymentCreated(PaymentCreatedEvent event) {
        handle("payment.created", () -> notificationService.sendPaymentCreated(event));
    }

    @RabbitListener(queues = RabbitMqConfig.PAYMENT_SUCCESS_QUEUE)
    public void onPaymentSuccess(PaymentSuccessEvent event) {
        handle("payment.success", () -> notificationService.sendPaymentSuccess(event));
    }

    @RabbitListener(queues = RabbitMqConfig.PAYMENT_FAILED_QUEUE)
    public void onPaymentFailed(PaymentFailedEvent event) {
        handle("payment.failed", () -> notificationService.sendPaymentFailed(event));
    }

    @RabbitListener(queues = RabbitMqConfig.PAYMENT_CANCELLED_QUEUE)
    public void onPaymentCancelled(PaymentCancelledEvent event) {
        handle("payment.cancelled", () -> notificationService.sendPaymentCancelled(event));
    }

    @RabbitListener(queues = RabbitMqConfig.PAYMENT_REFUNDED_QUEUE)
    public void onPaymentRefunded(PaymentRefundedEvent event) {
        handle("payment.refunded", () -> notificationService.sendPaymentRefunded(event));
    }

    private void handle(String eventName, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException exception) {
            log.error("Unable to process notification for {}", eventName, exception);
        }
    }
}
