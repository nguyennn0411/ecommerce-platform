package com.ecommerce.order.integration;

import java.math.BigDecimal;
import java.util.UUID;

public record ValidateProductItemRequest(
        UUID productId,
        BigDecimal unitPrice,
        Integer quantity
) {
}
