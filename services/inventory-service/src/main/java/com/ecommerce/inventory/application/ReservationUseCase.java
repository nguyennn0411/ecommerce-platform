package com.ecommerce.inventory.application;

import com.ecommerce.inventory.api.dto.InventoryAdjustmentResponse;
import com.ecommerce.inventory.api.dto.InventoryReservationResponse;
import com.ecommerce.inventory.api.dto.ReserveInventoryItemRequest;
import com.ecommerce.inventory.api.dto.ReserveInventoryRequest;
import com.ecommerce.inventory.domain.InventoryItem;
import com.ecommerce.inventory.domain.InventoryItemStatus;
import com.ecommerce.inventory.domain.ReservationStatus;
import com.ecommerce.inventory.domain.StockReservation;
import com.ecommerce.inventory.domain.vo.Quantity;
import com.ecommerce.inventory.persistence.InventoryItemRepository;
import com.ecommerce.inventory.persistence.StockReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ReservationUseCase {

    private final InventoryItemRepository inventoryItemRepository;
    private final StockReservationRepository stockReservationRepository;

    public ReservationUseCase(
            InventoryItemRepository inventoryItemRepository,
            StockReservationRepository stockReservationRepository
    ) {
        this.inventoryItemRepository = inventoryItemRepository;
        this.stockReservationRepository = stockReservationRepository;
    }

    @Transactional
    public InventoryReservationResponse reserve(ReserveInventoryRequest request) {
        UUID orderId = request.orderId();

        if (stockReservationRepository.existsByOrderIdAndStatus(orderId, ReservationStatus.RESERVED)) {
            return new InventoryReservationResponse(true, "Stock already reserved for order " + orderId);
        }
        if (stockReservationRepository.existsByOrderIdAndStatus(orderId, ReservationStatus.CONFIRMED)) {
            return new InventoryReservationResponse(false, "Order " + orderId + " already confirmed inventory");
        }

        List<ResolvedLine> lines = new ArrayList<>();
        for (ReserveInventoryItemRequest item : request.items()) {
            ResolvedLine line = resolveLine(item);
            if (line == null) {
                return new InventoryReservationResponse(
                        false,
                        "No sellable stock for productId=%s size=%s color=%s"
                                .formatted(item.productId(), item.size(), normalizeColor(item.color()))
                );
            }
            lines.add(line);
        }

        List<StockReservation> reservations = new ArrayList<>();
        for (ResolvedLine line : lines) {
            line.item().reserve(Quantity.of(line.quantity()));
            reservations.add(StockReservation.reserved(
                    orderId,
                    line.productId(),
                    line.item(),
                    line.quantity()
            ));
        }

        inventoryItemRepository.saveAll(lines.stream().map(ResolvedLine::item).toList());
        stockReservationRepository.saveAll(reservations);

        return new InventoryReservationResponse(true, "Reserved stock for order " + orderId);
    }

    @Transactional
    public InventoryAdjustmentResponse confirm(UUID orderId) {
        List<StockReservation> reservations =
                stockReservationRepository.findByOrderIdAndStatus(orderId, ReservationStatus.RESERVED);

        if (reservations.isEmpty()) {
            if (stockReservationRepository.existsByOrderIdAndStatus(orderId, ReservationStatus.CONFIRMED)) {
                return new InventoryAdjustmentResponse(true, "Inventory already confirmed for order " + orderId);
            }
            return new InventoryAdjustmentResponse(false, "No reserved stock found for order " + orderId);
        }

        for (StockReservation reservation : reservations) {
            InventoryItem item = reservation.getInventoryItem();
            item.confirm(Quantity.of(reservation.getQuantity()));
            reservation.confirm();
        }

        return new InventoryAdjustmentResponse(true, "Confirmed inventory deduction for order " + orderId);
    }

    @Transactional
    public InventoryAdjustmentResponse release(UUID orderId, String reason) {
        List<StockReservation> reservations =
                stockReservationRepository.findByOrderIdAndStatus(orderId, ReservationStatus.RESERVED);

        if (reservations.isEmpty()) {
            if (!stockReservationRepository.existsByOrderId(orderId)) {
                return new InventoryAdjustmentResponse(false, "No reservation found for order " + orderId);
            }
            return new InventoryAdjustmentResponse(true, "No active reservation to release for order " + orderId);
        }

        for (StockReservation reservation : reservations) {
            InventoryItem item = reservation.getInventoryItem();
            item.release(Quantity.of(reservation.getQuantity()));
            reservation.release();
        }

        String suffix = reason == null || reason.isBlank() ? "" : " (" + reason.trim() + ")";
        return new InventoryAdjustmentResponse(true, "Released reserved stock for order " + orderId + suffix);
    }

    private ResolvedLine resolveLine(ReserveInventoryItemRequest item) {
        String size = item.size().trim();
        String color = normalizeColor(item.color());

        return inventoryItemRepository
                .findByProductIdAndSizeAndColor(item.productId(), size, color)
                .filter(this::isSellable)
                .filter(inventoryItem -> inventoryItem.getAvailableQuantity() >= item.quantity())
                .map(inventoryItem -> new ResolvedLine(item.productId(), inventoryItem, item.quantity()))
                .orElse(null);
    }

    private boolean isSellable(InventoryItem item) {
        return item.getStatus() == InventoryItemStatus.IN_STOCK;
    }

    private String normalizeColor(String color) {
        if (color == null) {
            return null;
        }
        String trimmed = color.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record ResolvedLine(UUID productId, InventoryItem item, int quantity) {
    }
}
