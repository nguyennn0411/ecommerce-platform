package com.ecommerce.productcatalog.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateProductRequest(
        @NotBlank String name,
        String brand,
        String description,
        UUID categoryId,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal basePrice,
        String status,
        @Valid List<ProductVariantRequest> variants,
        @Valid List<ProductImageRequest> images
) {
}
