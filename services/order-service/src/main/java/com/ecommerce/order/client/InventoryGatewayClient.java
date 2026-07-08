package com.ecommerce.order.client;

import com.ecommerce.order.config.InventoryServiceProperties;
import com.ecommerce.order.domain.Order;
import com.ecommerce.order.dto.InventoryReservationItemRequest;
import com.ecommerce.order.dto.InventoryReservationRequest;
import com.ecommerce.order.dto.InventoryReservationResponse;
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
public class InventoryGatewayClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final InventoryServiceProperties properties;

    public InventoryGatewayClient(RestTemplate restTemplate,
                                  ObjectMapper objectMapper,
                                  InventoryServiceProperties properties) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public InventoryReservationResponse reserve(Order order) {
        return post(buildReservationUrl(), order);
    }

    public InventoryReservationResponse release(Order order) {
        return post(buildReleaseUrl(), order);
    }

    private InventoryReservationResponse post(String url, Order order) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<InventoryReservationRequest> entity = new HttpEntity<>(toRequest(order), headers);

        try {
            JsonNode root = restTemplate.postForObject(url, entity, JsonNode.class);
            if (root == null || root.path("success").isMissingNode() || !root.path("success").asBoolean()) {
                throw new OrderIntegrationException(extractMessage(root, "Inventory service returned an unsuccessful response"));
            }
            JsonNode data = root.path("data");
            if (data.isMissingNode() || data.isNull()) {
                throw new OrderIntegrationException("Inventory service returned an empty data payload");
            }
            return objectMapper.treeToValue(data, InventoryReservationResponse.class);
        } catch (RestClientResponseException exception) {
            throw new OrderIntegrationException("Inventory service error: " + extractMessage(exception.getResponseBodyAsString(), exception.getStatusText()), exception);
        } catch (OrderIntegrationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new OrderIntegrationException("Unable to communicate with inventory service", exception);
        }
    }

    private InventoryReservationRequest toRequest(Order order) {
        return new InventoryReservationRequest(
                order.getId(),
                order.getItems().stream()
                        .map(item -> new InventoryReservationItemRequest(item.getProductId(), item.getProductName(), item.getQuantity()))
                        .toList()
        );
    }

    private String buildReservationUrl() {
        return properties.getBaseUrl() + properties.getReservationPath();
    }

    private String buildReleaseUrl() {
        return properties.getBaseUrl() + properties.getReleasePath();
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
