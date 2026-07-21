package com.ecommerce.order.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CancelOrderRequest(@NotNull UUID userId, String reason) {
}
