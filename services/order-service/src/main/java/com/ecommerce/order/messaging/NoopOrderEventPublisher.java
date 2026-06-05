package com.ecommerce.order.messaging;

import com.ecommerce.order.domain.Order;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "ecommerce.kafka", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoopOrderEventPublisher implements OrderEventPublisher {

    @Override
    public String publishOrderCreated(Order order) {
        return "Kafka disabled; OrderCreatedEvent was not published";
    }

    @Override
    public String publishPaymentProcessed(Order order, String paymentStatus, String transactionId) {
        return "Kafka disabled; PaymentProcessedEvent was not published";
    }
}
