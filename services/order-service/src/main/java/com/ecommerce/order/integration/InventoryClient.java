package com.ecommerce.order.integration;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(name = "inventory-service", path = "/api/v1/inventory")
public interface InventoryClient {

    @PostMapping("/reservations")
    InventoryReservationResponse reserveStock(@RequestBody ReserveInventoryRequest request);

    @PostMapping("/reservations/{orderId}/confirm")
    InventoryAdjustmentResponse confirmReservation(@PathVariable("orderId") UUID orderId);

    @PostMapping("/reservations/{orderId}/release")
    InventoryAdjustmentResponse releaseReservation(
            @PathVariable("orderId") UUID orderId,
            @RequestParam(value = "reason", required = false) String reason
    );
}
