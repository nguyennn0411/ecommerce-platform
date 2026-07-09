package com.ecommerce.payment.api;

import com.ecommerce.common.web.ApiResponse;
import com.ecommerce.payment.dto.CreatePayosPaymentRequest;
import com.ecommerce.payment.dto.CreatePayosPaymentResponse;
import com.ecommerce.payment.dto.PayosWebhookRequest;
import com.ecommerce.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/payments/payos")
public class PayosPaymentController {

    private final PaymentService paymentService;

    public PayosPaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/create")
    public ApiResponse<CreatePayosPaymentResponse> create(@Valid @RequestBody CreatePayosPaymentRequest request) {
        CreatePayosPaymentResponse response = paymentService.createPayment(request);
        return ApiResponse.ok(response, response.message());
    }

    @PostMapping("/webhook")
    public ApiResponse<Map<String, Object>> webhook(@Valid @RequestBody PayosWebhookRequest request) {
        Map<String, Object> response = paymentService.handleWebhook(request);
        return ApiResponse.ok(response, messageFrom(response));
    }

    @GetMapping("/return")
    public ApiResponse<Map<String, Object>> payosReturn(@RequestParam Map<String, String> params) {
        Map<String, Object> response = paymentService.handleReturn(params);
        return ApiResponse.ok(response, messageFrom(response));
    }

    @GetMapping("/cancel")
    public ApiResponse<Map<String, Object>> payosCancel(@RequestParam Map<String, String> params) {
        Map<String, Object> response = paymentService.handleCancel(params);
        return ApiResponse.ok(response, messageFrom(response));
    }

    private String messageFrom(Map<String, Object> response) {
        Object message = response == null ? null : response.get("message");
        return message instanceof String text && !text.isBlank() ? text : "OK";
    }
}
