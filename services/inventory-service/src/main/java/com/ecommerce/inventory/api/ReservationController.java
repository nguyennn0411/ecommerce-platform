package com.ecommerce.inventory.api;

import com.ecommerce.inventory.api.dto.InventoryAdjustmentResponse;
import com.ecommerce.inventory.api.dto.InventoryReservationResponse;
import com.ecommerce.inventory.api.dto.ReserveInventoryRequest;
import com.ecommerce.inventory.application.ReservationUseCase;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventory")
public class ReservationController {

    private final ReservationUseCase reservationUseCase;

    public ReservationController(ReservationUseCase reservationUseCase) {
        this.reservationUseCase = reservationUseCase;
    }

    @PostMapping("/reservations")
    public InventoryReservationResponse reserve(@Valid @RequestBody ReserveInventoryRequest request) {
        return reservationUseCase.reserve(request);
    }

    @PostMapping("/reservations/{orderId}/confirm")
    public InventoryAdjustmentResponse confirm(@PathVariable("orderId") UUID orderId) {
        return reservationUseCase.confirm(orderId);
    }

    @PostMapping("/reservations/{orderId}/release")
    public InventoryAdjustmentResponse release(
            @PathVariable("orderId") UUID orderId,
            @RequestParam(value = "reason", required = false) String reason
    ) {
        return reservationUseCase.release(orderId, reason);
    }
}
