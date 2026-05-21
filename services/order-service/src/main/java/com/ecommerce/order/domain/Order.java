package com.ecommerce.order.domain;

import java.util.UUID;

public record Order(
        UUID id,
        String name
) {
}
