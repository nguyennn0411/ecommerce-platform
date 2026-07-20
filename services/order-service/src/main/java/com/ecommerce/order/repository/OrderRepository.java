package com.ecommerce.order.repository;

import com.ecommerce.order.domain.Order;
import com.ecommerce.order.domain.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    // My Orders: lấy đơn của một user, đơn mới nhất nằm trên.
    List<Order> findByUserIdOrderByCreatedAtDesc(UUID userId);
    // Timeout: lấy đơn còn PAYMENT_PENDING và tạo trước mốc thời gian.
    List<Order> findByStatusAndCreatedAtBefore(OrderStatus status, LocalDateTime createdAt);
}
