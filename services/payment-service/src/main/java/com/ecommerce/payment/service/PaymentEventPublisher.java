package com.ecommerce.payment.service;

import com.ecommerce.common.events.PaymentCancelledEvent;
import com.ecommerce.common.events.PaymentCreatedEvent;
import com.ecommerce.common.events.PaymentFailedEvent;
import com.ecommerce.common.events.PaymentRefundedEvent;
import com.ecommerce.common.events.PaymentSuccessEvent;
import com.ecommerce.payment.config.RabbitMqConfig;
import com.ecommerce.payment.entity.Payment;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;

@Service
public class PaymentEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public PaymentEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishPaymentCreated(Payment payment) {
        publish(RabbitMqConfig.PAYMENT_CREATED_ROUTING_KEY, new PaymentCreatedEvent(
                "PAYMENT_CREATED",
                payment.getOrderId(),
                payment.getOrderCode(),
                payment.getId(),
                payment.getUserId(),
                payment.getBuyerName(),
                payment.getBuyerEmail(),
                payment.getPaymentLinkId(),
                payment.getCheckoutUrl(),
                payment.getQrCode(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getPaymentProvider().name(),
                Instant.now()
        ));
    }

    public void publishPaymentSuccess(Payment payment, String providerReference) {
        publish(RabbitMqConfig.PAYMENT_SUCCESS_ROUTING_KEY, new PaymentSuccessEvent(
                "PAYMENT_SUCCESS",
                payment.getOrderId(),
                payment.getOrderCode(),
                payment.getId(),
                payment.getUserId(),
                payment.getBuyerName(),
                payment.getBuyerEmail(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getPaymentProvider().name(),
                payment.getPaymentLinkId(),
                providerReference,
                Instant.now()
        ));
    }

    public void publishPaymentFailed(Payment payment, String reason) {
        publish(RabbitMqConfig.PAYMENT_FAILED_ROUTING_KEY, new PaymentFailedEvent(
                "PAYMENT_FAILED",
                payment.getOrderId(),
                payment.getOrderCode(),
                payment.getId(),
                payment.getUserId(),
                payment.getBuyerName(),
                payment.getBuyerEmail(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getPaymentProvider().name(),
                reason,
                Instant.now()
        ));
    }

    public void publishPaymentCancelled(Payment payment, String reason) {
        publish(RabbitMqConfig.PAYMENT_CANCELLED_ROUTING_KEY, new PaymentCancelledEvent(
                "PAYMENT_CANCELLED",
                payment.getOrderId(),
                payment.getOrderCode(),
                payment.getId(),
                payment.getUserId(),
                payment.getBuyerName(),
                payment.getBuyerEmail(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getPaymentProvider().name(),
                reason,
                Instant.now()
        ));
    }

    public void publishPaymentRefunded(Payment payment) {
        publish(RabbitMqConfig.PAYMENT_REFUNDED_ROUTING_KEY, new PaymentRefundedEvent(
                "PAYMENT_REFUNDED",
                payment.getOrderId(),
                payment.getOrderCode(),
                payment.getId(),
                payment.getUserId(),
                payment.getBuyerName(),
                payment.getBuyerEmail(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getPaymentProvider().name(),
                payment.getPaymentLinkId(),
                null,
                Instant.now()
        ));
    }

    private void publish(String routingKey, Object event) {
        Runnable publishAction = () -> rabbitTemplate.convertAndSend(RabbitMqConfig.ECOMMERCE_EXCHANGE, routingKey, event);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publishAction.run();
                }
            });
        } else {
            publishAction.run();
        }
    }
}
