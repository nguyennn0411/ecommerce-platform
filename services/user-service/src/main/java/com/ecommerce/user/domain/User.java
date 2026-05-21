package com.ecommerce.user.domain;

import java.util.UUID;

public record User(
        UUID id,
        String name
) {
}
