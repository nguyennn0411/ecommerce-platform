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

    public PaymentCreateResponse createPayment(PaymentCreateRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<PaymentCreateRequest> entity = new HttpEntity<>(request, headers);

        try {
            JsonNode root = restTemplate.postForObject(buildCreateUrl(), entity, JsonNode.class);
            if (root == null || root.path("success").isMissingNode() || !root.path("success").asBoolean()) {
                throw new OrderIntegrationException(extractMessage(root, "Payment service returned an unsuccessful response"));
            }
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

    private String buildCreateUrl() {
        return properties.getBaseUrl() + properties.getCreatePayosPath();
    }

    private String extractMessage(JsonNode root, String fallback) {
        if (root != null) {
            JsonNode message = root.get("message");
            if (message != null && !message.isNull() && !message.asText().isBlank()) {
                return message.asText();
            }
        }
        return fallback;
    }

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
