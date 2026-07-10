package com.ecommerce.productcatalog.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ValidateProductItemRequest(
        UUID productId,
        BigDecimal unitPrice,
        Integer quantity
) {
}
