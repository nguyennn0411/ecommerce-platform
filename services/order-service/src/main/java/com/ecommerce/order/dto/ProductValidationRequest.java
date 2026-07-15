package com.ecommerce.order.dto;

import java.util.List;

public record ProductValidationRequest(
        List<ProductValidationItemRequest> items
) {
}
