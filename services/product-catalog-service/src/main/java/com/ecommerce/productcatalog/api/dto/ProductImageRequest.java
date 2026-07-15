package com.ecommerce.productcatalog.api.dto;

import jakarta.validation.constraints.NotBlank;

public record ProductImageRequest(
        @NotBlank String imageUrl,
        Boolean main
) {
}
