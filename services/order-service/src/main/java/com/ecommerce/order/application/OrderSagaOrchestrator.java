package com.ecommerce.order.application;

import com.ecommerce.order.client.InventoryGatewayClient;
import com.ecommerce.order.client.PaymentGatewayClient;
import com.ecommerce.order.domain.Order;
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

    public void reserveInventoryFor(Order order) {
        // Buoc 1: giu hang tam thoi; kho chua bi tru that.
        inventoryGatewayClient.reserve(order);
    }

    public void confirmInventoryFor(Order order) {
        // Buoc 3: chi tru kho that sau khi Payment thong bao thanh cong.
        inventoryGatewayClient.confirm(order);
    }

    public void releaseInventoryFor(Order order) {
        // Compensation: tra hang da giu khi payment fail, huy, hoac qua han.
        inventoryGatewayClient.release(order);
    }

    public PaymentCreateResponse createPaymentFor(Order order) {
        // Buoc 2: Payment tao giao dich PayOS va tra ve checkout URL cho FE.
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
