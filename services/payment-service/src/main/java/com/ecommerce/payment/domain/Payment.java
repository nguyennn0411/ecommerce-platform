package com.ecommerce.payment.domain;

import java.util.UUID;

public record Payment(
        UUID id,
        String name
) {
}
