package com.ecommerce.notification.domain;

import java.util.UUID;

public record Notification(
        UUID id,
        String name
) {
}
