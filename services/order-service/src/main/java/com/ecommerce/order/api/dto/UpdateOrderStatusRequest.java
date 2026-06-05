package com.ecommerce.order.api.dto;

import com.ecommerce.order.domain.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(
        @NotNull OrderStatus status,
        String reason
) {
}
