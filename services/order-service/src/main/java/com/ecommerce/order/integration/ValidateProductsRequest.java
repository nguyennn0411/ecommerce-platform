package com.ecommerce.order.integration;

import java.util.List;

public record ValidateProductsRequest(
        List<ValidateProductItemRequest> items
) {
}
