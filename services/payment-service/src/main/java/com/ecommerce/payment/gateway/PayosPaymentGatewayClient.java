package com.ecommerce.payment.gateway;

import com.ecommerce.payment.config.PayosProperties;
import com.ecommerce.payment.dto.ConfirmPayosWebhookResponse;
import com.ecommerce.payment.dto.PayosCreatePaymentRequest;
import com.ecommerce.payment.dto.PayosCreatePaymentResponse;
import com.ecommerce.payment.dto.PayosWebhookRequest;
import com.ecommerce.payment.exception.GatewayException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import vn.payos.PayOS;
import vn.payos.model.webhooks.ConfirmWebhookResponse;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class PayosPaymentGatewayClient implements PaymentGatewayClient {

    private static final Logger log = LoggerFactory.getLogger(PayosPaymentGatewayClient.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final PayosProperties payosProperties;
    private final PayOS payOS;

    public PayosPaymentGatewayClient(RestTemplate restTemplate,
                                     ObjectMapper objectMapper,
                                     PayosProperties payosProperties,
                                     PayOS payOS) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.payosProperties = payosProperties;
        this.payOS = payOS;
    }

    @Override
    public PayosCreatePaymentResponse createPayment(PayosCreatePaymentRequest request) {
        requirePayosCredentials();
        return postPaymentRequest(request, "/v2/payment-requests");
    }

    @Override
    public PayosCreatePaymentResponse getPaymentStatus(Long orderCode) {
        requirePayosCredentials();
        return getPaymentRequest("/v2/payment-requests/%s".formatted(orderCode));
    }

    @Override
    public PayosCreatePaymentResponse cancelPayment(Long orderCode, String reason) {
        requirePayosCredentials();
        Map<String, String> body = new LinkedHashMap<>();
        body.put("cancellationReason", reason);
        return postPaymentRequest(body, "/v2/payment-requests/%s/cancel".formatted(orderCode));
    }

    @Override
    public boolean verifyWebhookSignature(PayosWebhookRequest request) {
        requirePayosCredentials();
        if (request == null || request.data() == null) {
            return false;
        }
        try {
            Map<String, Object> payload = objectMapper.convertValue(request, new TypeReference<>() {
            });
            payOS.webhooks().verify(payload);
            return true;
        } catch (Exception exception) {
            log.warn("PayOS SDK rejected webhook signature: {}", exception.getMessage());
            return false;
        }
    }

    @Override
    public ConfirmPayosWebhookResponse confirmWebhook(String webhookUrl) {
        requirePayosCredentials();
        try {
            ConfirmWebhookResponse response = payOS.webhooks().confirm(webhookUrl);
            return new ConfirmPayosWebhookResponse(
                    response.getWebhookUrl(),
                    response.getName(),
                    response.getShortName()
            );
        } catch (Exception exception) {
            throw new GatewayException("Unable to confirm PayOS webhook URL", exception);
        }
    }

    private PayosCreatePaymentResponse postPaymentRequest(Object body, String path) {
        try {
            HttpHeaders headers = headers();
            ResponseEntity<PayosCreatePaymentResponse> response = restTemplate.postForEntity(
                    payosProperties.getBaseUrl() + path,
                    new HttpEntity<>(body, headers),
                    PayosCreatePaymentResponse.class
            );
            return requireBody(response);
        } catch (RestClientException exception) {
            throw new GatewayException("Unable to call PayOS payment API", exception);
        }
    }

    private PayosCreatePaymentResponse getPaymentRequest(String path) {
        try {
            HttpHeaders headers = headers();
            ResponseEntity<PayosCreatePaymentResponse> response = restTemplate.exchange(
                    payosProperties.getBaseUrl() + path,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    PayosCreatePaymentResponse.class
            );
            return requireBody(response);
        } catch (RestClientException exception) {
            throw new GatewayException("Unable to fetch PayOS payment status", exception);
        }
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set("x-client-id", payosProperties.getClientId());
        headers.set("x-api-key", payosProperties.getApiKey());
        return headers;
    }

    private PayosCreatePaymentResponse requireBody(ResponseEntity<PayosCreatePaymentResponse> response) {
        PayosCreatePaymentResponse body = response.getBody();
        if (body == null) {
            throw new GatewayException("PayOS returned an empty response");
        }
        return body;
    }

    @SuppressWarnings("unused")
    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new GatewayException("Unable to serialize PayOS response", exception);
        }
    }

    private void requirePayosCredentials() {
        if (!payosProperties.hasCredentials()) {
            throw new GatewayException("PayOS credentials are not configured. Set PAYOS_CLIENT_ID, PAYOS_API_KEY, and PAYOS_CHECKSUM_KEY.");
        }
    }
}
