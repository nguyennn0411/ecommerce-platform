package com.ecommerce.inventory.domain;

import java.util.UUID;

public record Inventory(
        UUID id,
        String name
) {
}
