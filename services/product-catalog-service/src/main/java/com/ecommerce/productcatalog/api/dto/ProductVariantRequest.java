package com.ecommerce.productcatalog.api.dto;

import jakarta.validation.constraints.NotBlank;

public record ProductVariantRequest(
        @NotBlank String size,
        String color,
        String sku
) {
}
