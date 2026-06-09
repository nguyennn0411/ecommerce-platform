package com.ecommerce.notification.repository;

import com.ecommerce.notification.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByRecipientIgnoreCaseOrderByCreatedAtDesc(String recipient, Pageable pageable);

    List<Notification> findByOrderIdOrderByCreatedAtDesc(UUID orderId);
}
