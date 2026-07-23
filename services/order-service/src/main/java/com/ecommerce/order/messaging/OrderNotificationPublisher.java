package com.ecommerce.order.messaging;

import com.ecommerce.order.domain.Order;
import com.ecommerce.order.config.RabbitMqConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class OrderNotificationPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderNotificationPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public OrderNotificationPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    // Phát event order.* cho Notification Service để gửi email/thông báo cho khách.
    public void publish(Order order, String eventType, String message) {
        OrderNotificationEvent event = new OrderNotificationEvent(
                order.getId(), order.getUserId(), order.getBuyerEmail(), eventType,
                order.getStatus().name(), message, Instant.now()
        );
        try {
            rabbitTemplate.convertAndSend(RabbitMqConfig.ECOMMERCE_EXCHANGE, "order." + eventType, event);
        } catch (RuntimeException exception) {
            // Notification là side effect; Rabbit lỗi không được làm lỗi hoặc treo Saga tạo đơn.
            log.error("Could not publish order.{} notification for order {}", eventType, order.getId(), exception);
        }
    }
}
