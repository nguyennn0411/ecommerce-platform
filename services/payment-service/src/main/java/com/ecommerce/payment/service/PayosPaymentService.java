package com.ecommerce.payment.service;

import com.ecommerce.payment.dto.PayosCreatePaymentRequest;
import com.ecommerce.payment.dto.PayosCreatePaymentResponse;
import com.ecommerce.payment.dto.PayosWebhookRequest;

public interface PayosPaymentService {

    PayosCreatePaymentResponse createPayment(PayosCreatePaymentRequest request);

    PayosCreatePaymentResponse getPaymentStatus(Long orderCode);

    PayosCreatePaymentResponse cancelPayment(Long orderCode, String reason);

    boolean verifyWebhookSignature(PayosWebhookRequest request);
}
