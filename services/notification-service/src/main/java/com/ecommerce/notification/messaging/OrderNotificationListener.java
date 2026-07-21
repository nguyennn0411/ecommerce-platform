package com.ecommerce.notification.messaging;

import com.ecommerce.notification.config.RabbitMqConfig;
import com.ecommerce.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class OrderNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(OrderNotificationListener.class);

    private final NotificationService notificationService;

    public OrderNotificationListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = RabbitMqConfig.ORDER_NOTIFICATION_QUEUE)
    public void onOrderEvent(OrderNotificationEvent event) {
        try {
            notificationService.sendOrderNotification(event);
        } catch (RuntimeException exception) {
            log.error("Unable to process notification for order event {}", event.eventType(), exception);
        }
    }
}
