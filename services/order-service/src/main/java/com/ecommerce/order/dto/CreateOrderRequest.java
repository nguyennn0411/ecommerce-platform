package com.ecommerce.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateOrderRequest(
        @NotNull UUID userId,
        @NotBlank String buyerName,
        @NotBlank @Email String buyerEmail,
        String description,
        String currency,
        @DecimalMin(value = "0.00", message = "shippingFee must not be negative") BigDecimal shippingFee,
        @Valid @NotEmpty List<CreateOrderItemRequest> items
) {
}
