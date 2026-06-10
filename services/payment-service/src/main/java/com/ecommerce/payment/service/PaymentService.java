package com.ecommerce.payment.service;

import com.ecommerce.payment.dto.CancelPaymentRequest;
import com.ecommerce.payment.dto.CreatePayosPaymentRequest;
import com.ecommerce.payment.dto.CreatePayosPaymentResponse;
import com.ecommerce.payment.dto.PayosWebhookRequest;
import com.ecommerce.payment.dto.PaymentResponse;

import java.util.Map;
import java.util.UUID;

public interface PaymentService {

    CreatePayosPaymentResponse createPayment(CreatePayosPaymentRequest request);

    Map<String, Object> handleWebhook(PayosWebhookRequest request);

    Map<String, Object> handleReturn(Map<String, String> params);

    Map<String, Object> handleCancel(Map<String, String> params);

    PaymentResponse getPaymentByOrderId(UUID orderId);

    PaymentResponse getPaymentById(UUID paymentId);

    PaymentResponse cancelPayment(UUID paymentId, CancelPaymentRequest request);
}
