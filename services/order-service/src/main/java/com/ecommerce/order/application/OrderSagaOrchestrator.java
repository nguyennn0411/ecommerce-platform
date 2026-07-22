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

        // Bước 1: giữ hàng tạm thời; kho chưa bị trừ thật.
    public void reserveInventoryFor(Order order) {

        inventoryGatewayClient.reserve(order);
    }
    // Thanh toán thành công thì Inventory trừ kho thật.
    public void confirmInventoryFor(Order order) {
        inventoryGatewayClient.confirm(order);
    }

    // Thanh toán fail/hủy thì trả lượng hàng đã giữ.
    public void releaseInventoryFor(Order order) {
        inventoryGatewayClient.release(order);
    }

    // Gửi thông tin đơn sang Payment để tạo link PayOS.
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
