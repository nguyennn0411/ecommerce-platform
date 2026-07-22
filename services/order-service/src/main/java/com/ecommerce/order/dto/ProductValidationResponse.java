package com.ecommerce.order.dto;

public record ProductValidationResponse(
        boolean valid,
        String message
) {
}
