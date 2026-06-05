package com.ecommerce.order.messaging;

import com.ecommerce.common.events.OrderCreatedEvent;
import com.ecommerce.common.events.PaymentProcessedEvent;
import com.ecommerce.order.domain.Order;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;

@Service
@ConditionalOnProperty(prefix = "ecommerce.kafka", name = "enabled", havingValue = "true")
public class KafkaOrderEventPublisher implements OrderEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String orderCreatedTopic;
    private final String paymentProcessedTopic;

    public KafkaOrderEventPublisher(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${ecommerce.kafka.topics.order-created:order.created}") String orderCreatedTopic,
            @Value("${ecommerce.kafka.topics.payment-processed:payment.processed}") String paymentProcessedTopic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.orderCreatedTopic = orderCreatedTopic;
        this.paymentProcessedTopic = paymentProcessedTopic;
    }

    @Override
    public String publishOrderCreated(Order order) {
        Instant occurredAt = order.getCreatedAt() == null
                ? Instant.now()
                : order.getCreatedAt().toInstant(ZoneOffset.UTC);
        OrderCreatedEvent event = new OrderCreatedEvent(
                order.getId(),
                order.getUserId(),
                order.getTotalAmount(),
                occurredAt
        );
        kafkaTemplate.send(orderCreatedTopic, order.getId().toString(), event);
        return "Published OrderCreatedEvent to " + orderCreatedTopic;
    }

    @Override
    public String publishPaymentProcessed(Order order, String paymentStatus, String transactionId) {
        PaymentProcessedEvent event = new PaymentProcessedEvent(
                order.getId(),
                paymentStatus,
                transactionId,
                Instant.now()
        );
        kafkaTemplate.send(paymentProcessedTopic, order.getId().toString(), event);
        return "Published PaymentProcessedEvent to " + paymentProcessedTopic;
    }
}
