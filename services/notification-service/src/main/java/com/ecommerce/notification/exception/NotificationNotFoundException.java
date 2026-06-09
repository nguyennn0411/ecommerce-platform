package com.ecommerce.notification.exception;

import java.util.UUID;

public class NotificationNotFoundException extends RuntimeException {

    public NotificationNotFoundException(UUID notificationId) {
        super("Notification not found for notificationId=%s".formatted(notificationId));
    }
}
