package com.ecommerce.order.api.dto;

import com.ecommerce.order.domain.SagaTransactionStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record SagaTransactionResponse(
        UUID id,
        String sagaType,
        String currentStep,
        SagaTransactionStatus status,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<SagaStepResponse> steps
) {
}
