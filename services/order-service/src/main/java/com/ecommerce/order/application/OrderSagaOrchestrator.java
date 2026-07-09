package com.ecommerce.order.application;

import com.ecommerce.order.client.InventoryGatewayClient;
import com.ecommerce.order.client.PaymentGatewayClient;
import com.ecommerce.order.domain.Order;
import com.ecommerce.order.dto.InventoryAdjustmentResponse;
import com.ecommerce.order.dto.InventoryReservationResponse;
import com.ecommerce.order.dto.PaymentCreateRequest;
import com.ecommerce.order.dto.PaymentCreateResponse;
import org.springframework.stereotype.Service;

@Service
public class OrderSagaOrchestrator {

    private final InventoryGatewayClient inventoryGatewayClient;
    private final PaymentGatewayClient paymentGatewayClient;

    public OrderSagaOrchestrator(InventoryGatewayClient inventoryGatewayClient,
                                 PaymentGatewayClient paymentGatewayClient) {
        this.inventoryGatewayClient = inventoryGatewayClient;
        this.paymentGatewayClient = paymentGatewayClient;
    }

    public InventoryReservationResponse reserveInventoryFor(Order order) {
        return inventoryGatewayClient.reserve(order);
    }

    public InventoryAdjustmentResponse releaseInventoryFor(Order order) {
        return inventoryGatewayClient.release(order);
    }

    public InventoryAdjustmentResponse confirmInventoryFor(Order order) {
        return inventoryGatewayClient.confirm(order.getId());
    }

    public PaymentCreateResponse createPaymentFor(Order order) {
        return paymentGatewayClient.createPayment(new PaymentCreateRequest(
                order.getId(),
                order.getUserId(),
                order.getTotalAmount(),
                order.getCurrency(),
                order.getDescription(),
                order.getBuyerName(),
                order.getBuyerEmail(),
                null,
                null
        ));
    }
}
