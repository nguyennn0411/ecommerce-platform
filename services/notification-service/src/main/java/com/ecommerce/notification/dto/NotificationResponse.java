package com.ecommerce.notification.dto;

import com.ecommerce.notification.entity.Notification;
import com.ecommerce.notification.enums.NotificationChannel;
import com.ecommerce.notification.enums.NotificationStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record NotificationResponse(
        UUID notificationId,
        UUID userId,
        UUID orderId,
        NotificationChannel channel,
        String recipient,
        String subject,
        String content,
        NotificationStatus status,
        String errorMessage,
        LocalDateTime sentAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getUserId(),
                notification.getOrderId(),
                notification.getChannel(),
                notification.getRecipient(),
                notification.getSubject(),
                notification.getContent(),
                notification.getStatus(),
                notification.getErrorMessage(),
                notification.getSentAt(),
                notification.getCreatedAt(),
                notification.getUpdatedAt()
        );
    }
}
