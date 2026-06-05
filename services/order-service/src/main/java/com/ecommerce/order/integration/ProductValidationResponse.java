package com.ecommerce.order.integration;

public record ProductValidationResponse(
        boolean valid,
        String message
) {
}
