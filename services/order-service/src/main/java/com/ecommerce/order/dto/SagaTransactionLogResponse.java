package com.ecommerce.order.dto;

import com.ecommerce.order.domain.SagaLogStatus;
import com.ecommerce.order.domain.SagaTransactionLog;

import java.time.LocalDateTime;
import java.util.UUID;

public record SagaTransactionLogResponse(
        UUID logId,
        UUID orderId,
        String step,
        SagaLogStatus status,
        String message,
        LocalDateTime createdAt
) {
    public static SagaTransactionLogResponse from(SagaTransactionLog log) {
        return new SagaTransactionLogResponse(
                log.getId(), log.getOrderId(), log.getStep(), log.getStatus(), log.getMessage(), log.getCreatedAt()
        );
    }
}
