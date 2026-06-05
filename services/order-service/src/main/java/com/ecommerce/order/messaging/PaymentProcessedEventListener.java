package com.ecommerce.order.messaging;

import com.ecommerce.common.events.PaymentProcessedEvent;
import com.ecommerce.order.api.dto.CompletePaymentRequest;
import com.ecommerce.order.application.OrderUseCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "ecommerce.kafka", name = "enabled", havingValue = "true")
public class PaymentProcessedEventListener {

    private final OrderUseCase orderUseCase;

    public PaymentProcessedEventListener(OrderUseCase orderUseCase) {
        this.orderUseCase = orderUseCase;
    }

    @KafkaListener(
            topics = "${ecommerce.kafka.topics.payment-processed:payment.processed}",
            groupId = "${spring.application.name:order-service}"
    )
    public void onPaymentProcessed(PaymentProcessedEvent event) {
        CompletePaymentRequest request = new CompletePaymentRequest(
                null,
                event.paymentStatus(),
                event.transactionId()
        );
        orderUseCase.completePayment(event.orderId(), request);
    }
}
