package com.ecommerce.order.application;

import com.ecommerce.order.domain.SagaLogStatus;
import com.ecommerce.order.domain.SagaTransactionLog;
import com.ecommerce.order.dto.SagaTransactionLogResponse;
import com.ecommerce.order.repository.SagaTransactionLogRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class SagaLogService {

    private final SagaTransactionLogRepository repository;

    public SagaLogService(SagaTransactionLogRepository repository) {
        this.repository = repository;
    }

    public void write(UUID orderId, String step, SagaLogStatus status, String message) {
        repository.save(new SagaTransactionLog(orderId, step, status, message));
    }

    public List<SagaTransactionLogResponse> findByOrderId(UUID orderId) {
        return repository.findByOrderIdOrderByCreatedAtAsc(orderId).stream()
                .map(SagaTransactionLogResponse::from)
                .toList();
    }
}
