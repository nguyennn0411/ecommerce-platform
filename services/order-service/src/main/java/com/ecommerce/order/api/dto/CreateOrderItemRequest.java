package com.ecommerce.order.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateOrderItemRequest(
        @NotNull UUID productId,
        @NotBlank String productName,
        @NotNull @DecimalMin("0.00") BigDecimal unitPrice,
        @NotNull @Positive Integer quantity
) {
}
