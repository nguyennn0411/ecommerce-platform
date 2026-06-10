package com.ecommerce.payment.service.impl;

import com.ecommerce.payment.dto.PayosCreatePaymentRequest;
import com.ecommerce.payment.dto.PayosCreatePaymentResponse;
import com.ecommerce.payment.dto.PayosWebhookRequest;
import com.ecommerce.payment.gateway.PaymentGatewayClient;
import com.ecommerce.payment.service.PayosPaymentService;
import org.springframework.stereotype.Service;

@Service
public class PayosPaymentServiceImpl implements PayosPaymentService {

    private final PaymentGatewayClient paymentGatewayClient;

    public PayosPaymentServiceImpl(PaymentGatewayClient paymentGatewayClient) {
        this.paymentGatewayClient = paymentGatewayClient;
    }

    @Override
    public PayosCreatePaymentResponse createPayment(PayosCreatePaymentRequest request) {
        return paymentGatewayClient.createPayment(request);
    }

    @Override
    public PayosCreatePaymentResponse getPaymentStatus(Long orderCode) {
        return paymentGatewayClient.getPaymentStatus(orderCode);
    }

    @Override
    public PayosCreatePaymentResponse cancelPayment(Long orderCode, String reason) {
        return paymentGatewayClient.cancelPayment(orderCode, reason);
    }

    @Override
    public boolean verifyWebhookSignature(PayosWebhookRequest request) {
        return paymentGatewayClient.verifyWebhookSignature(request);
    }
}
