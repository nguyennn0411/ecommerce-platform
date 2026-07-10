package com.ecommerce.inventory.persistence;

import com.ecommerce.inventory.domain.ReservationStatus;
import com.ecommerce.inventory.domain.StockReservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StockReservationRepository extends JpaRepository<StockReservation, UUID> {

    List<StockReservation> findByOrderIdAndStatus(UUID orderId, ReservationStatus status);

    List<StockReservation> findByOrderId(UUID orderId);

    boolean existsByOrderId(UUID orderId);

    boolean existsByOrderIdAndStatus(UUID orderId, ReservationStatus status);
}
