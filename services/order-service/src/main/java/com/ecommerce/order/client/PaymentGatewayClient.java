package com.ecommerce.order.client;

import com.ecommerce.order.config.PaymentServiceProperties;
import com.ecommerce.order.dto.PaymentCreateRequest;
import com.ecommerce.order.dto.PaymentCreateResponse;
import com.ecommerce.order.exception.OrderIntegrationException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

@Component
public class PaymentGatewayClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final PaymentServiceProperties properties;

    public PaymentGatewayClient(RestTemplate restTemplate,
                                ObjectMapper objectMapper,
                                PaymentServiceProperties properties) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    // Gửi thông tin đơn sang Payment Service để tạo giao dịch PayOS và nhận checkoutUrl.
    public PaymentCreateResponse createPayment(PaymentCreateRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Đóng gói dữ liệu đơn rồi POST sang Payment Service.
        HttpEntity<PaymentCreateRequest> entity = new HttpEntity<>(request, headers);

        try {
            JsonNode root = restTemplate.postForObject(buildCreateUrl(), entity, JsonNode.class);

            // Payment trả success=false thì Order coi là lỗi.
            if (root == null || root.path("success").isMissingNode() || !root.path("success").asBoolean()) {
                throw new OrderIntegrationException(extractMessage(root, "Payment service returned an unsuccessful response"));
            }

            // Lấy phần data gồm paymentId, checkoutUrl, QR...
            JsonNode data = root.path("data");
            if (data.isMissingNode() || data.isNull()) {
                throw new OrderIntegrationException("Payment service returned an empty data payload");
            }
            return objectMapper.treeToValue(data, PaymentCreateResponse.class);
        } catch (RestClientResponseException exception) {
            throw new OrderIntegrationException("Payment service error: " + extractMessage(exception.getResponseBodyAsString(), exception.getStatusText()), exception);
        } catch (OrderIntegrationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new OrderIntegrationException("Unable to create payment for order", exception);
        }
    }

    // Ghép base URL Payment và path tạo PayOS từ application.yml.
    private String buildCreateUrl() {
        // Ghép base URL Payment với path tạo PayOS.
        return properties.getBaseUrl() + properties.getCreatePayosPath();
    }

    // Lấy message lỗi từ JSON wrapper Payment trả về.
    private String extractMessage(JsonNode root, String fallback) {
        if (root != null) {
            JsonNode message = root.get("message");
            if (message != null && !message.isNull() && !message.asText().isBlank()) {
                return message.asText();
            }
        }
        return fallback;
    }

    // Parse body lỗi dạng chuỗi rồi lấy message để Order hiển thị nguyên nhân rõ hơn.
    private String extractMessage(String body, String fallback) {
        if (body == null || body.isBlank()) {
            return fallback;
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            return extractMessage(root, fallback);
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
