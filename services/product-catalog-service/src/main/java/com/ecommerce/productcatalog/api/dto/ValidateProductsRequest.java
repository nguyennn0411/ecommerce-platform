package com.ecommerce.productcatalog.api.dto;

import java.util.List;

public record ValidateProductsRequest(
        List<ValidateProductItemRequest> items
) {
}
