package com.ecommerce.order.persistence;

import com.ecommerce.order.domain.SagaTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SagaTransactionRepository extends JpaRepository<SagaTransaction, UUID> {

    List<SagaTransaction> findByOrder_IdOrderByCreatedAtDesc(UUID orderId);
}
