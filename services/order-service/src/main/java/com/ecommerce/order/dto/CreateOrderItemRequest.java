package com.ecommerce.order.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateOrderItemRequest(
        UUID productId,
        @NotBlank String productName,
        @NotBlank String size,
        String color,
        @Min(1) int quantity,
        @NotNull
        @DecimalMin(value = "0.01", message = "unitPrice must be greater than 0") BigDecimal unitPrice
) {
}
