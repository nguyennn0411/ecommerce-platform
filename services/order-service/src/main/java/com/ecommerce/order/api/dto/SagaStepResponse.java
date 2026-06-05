package com.ecommerce.order.api.dto;

import com.ecommerce.order.domain.SagaStepStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record SagaStepResponse(
        UUID id,
        String stepName,
        SagaStepStatus status,
        String requestPayload,
        String responsePayload,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
