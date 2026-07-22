package com.ecommerce.order.repository;

import com.ecommerce.order.domain.SagaTransactionLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SagaTransactionLogRepository extends JpaRepository<SagaTransactionLog, UUID> {
    List<SagaTransactionLog> findByOrderIdOrderByCreatedAtAsc(UUID orderId);
}
