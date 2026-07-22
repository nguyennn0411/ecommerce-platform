package com.ecommerce.order.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductValidationItemRequest(
        UUID productId,
        BigDecimal unitPrice,
        Integer quantity
) {
}
