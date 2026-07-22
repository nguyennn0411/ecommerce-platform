package com.ecommerce.notification.service;

import com.ecommerce.common.events.PaymentCancelledEvent;
import com.ecommerce.common.events.PaymentCreatedEvent;
import com.ecommerce.common.events.PaymentFailedEvent;
import com.ecommerce.common.events.PaymentRefundedEvent;
import com.ecommerce.common.events.PaymentSuccessEvent;
import com.ecommerce.notification.dto.MailConfigurationResponse;
import com.ecommerce.notification.dto.NotificationResponse;
import com.ecommerce.notification.dto.SendEmailRequest;
import com.ecommerce.notification.messaging.OrderNotificationEvent;

import java.util.List;
import java.util.UUID;

public interface NotificationService {

    NotificationResponse sendEmail(SendEmailRequest request);

    NotificationResponse getNotification(UUID notificationId);

    List<NotificationResponse> getByRecipient(String recipient, int limit);

    List<NotificationResponse> getByOrderId(UUID orderId);

    MailConfigurationResponse getMailConfiguration();

    void sendPaymentCreated(PaymentCreatedEvent event);

    void sendPaymentSuccess(PaymentSuccessEvent event);

    void sendPaymentFailed(PaymentFailedEvent event);

    void sendPaymentCancelled(PaymentCancelledEvent event);

    void sendPaymentRefunded(PaymentRefundedEvent event);

    void sendOrderNotification(OrderNotificationEvent event);
}
