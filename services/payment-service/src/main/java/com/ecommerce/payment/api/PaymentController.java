package com.ecommerce.payment.api;

import com.ecommerce.common.web.ApiResponse;
import com.ecommerce.payment.dto.CancelPaymentRequest;
import com.ecommerce.payment.dto.PaymentResponse;
import com.ecommerce.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/{paymentId}")
    public ApiResponse<PaymentResponse> getById(@PathVariable UUID paymentId) {
        return ok(paymentService.getPaymentById(paymentId));
    }

    @GetMapping("/orders/{orderId}")
    public ApiResponse<PaymentResponse> getByOrderId(@PathVariable UUID orderId) {
        return ok(paymentService.getPaymentByOrderId(orderId));
    }

    @PostMapping("/{paymentId}/cancel")
    public ApiResponse<PaymentResponse> cancel(@PathVariable UUID paymentId,
                                               @Valid @RequestBody(required = false) CancelPaymentRequest request) {
        return ok(paymentService.cancelPayment(paymentId, request));
    }

    private <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, "OK", Instant.now());
    }
}
