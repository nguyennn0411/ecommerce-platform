package com.ecommerce.order.messaging;

import com.ecommerce.order.domain.Order;

public interface OrderEventPublisher {

    String publishOrderCreated(Order order);

    String publishPaymentProcessed(Order order, String paymentStatus, String transactionId);
}
