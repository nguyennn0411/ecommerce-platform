package com.ecommerce.order.client;

import com.ecommerce.order.config.InventoryServiceProperties;
import com.ecommerce.order.domain.Order;
import com.ecommerce.order.dto.InventoryAdjustmentResponse;
import com.ecommerce.order.dto.InventoryReservationItemRequest;
import com.ecommerce.order.dto.InventoryReservationRequest;
import com.ecommerce.order.dto.InventoryReservationResponse;
import com.ecommerce.order.exception.OrderIntegrationException;
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

    public void reserve(Order order) {
        InventoryReservationResponse response = reserveInventory(buildReservationUrl(), order);
        if (response == null || !response.reserved()) {
            throw new OrderIntegrationException(response == null || response.message() == null || response.message().isBlank()
                    ? "Inventory reservation failed"
                    : response.message());
        }
    }

    public void confirm(Order order) {
        InventoryAdjustmentResponse response = adjustInventory(buildConfirmUrl(order), null);
        validateAdjustment(response, "Inventory confirmation failed");
    }

    public void release(Order order) {
        InventoryAdjustmentResponse response = adjustInventory(buildReleaseUrl(order), "payment-failed");
        validateAdjustment(response, "Inventory release failed");
    }

    private InventoryReservationResponse reserveInventory(String url, Order order) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<InventoryReservationRequest> entity = new HttpEntity<>(toRequest(order), headers);

        try {
            return restTemplate.postForObject(url, entity, InventoryReservationResponse.class);
        } catch (RestClientResponseException exception) {
            throw new OrderIntegrationException("Inventory service error: " + extractMessage(exception.getResponseBodyAsString(), exception.getStatusText()), exception);
        } catch (OrderIntegrationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new OrderIntegrationException("Unable to communicate with inventory service", exception);
        }
    }

    private InventoryAdjustmentResponse adjustInventory(String url, String reason) {
        String finalUrl = reason == null || reason.isBlank() ? url : url + "?reason=" + reason;

        try {
            return restTemplate.postForObject(finalUrl, HttpEntity.EMPTY, InventoryAdjustmentResponse.class);
        } catch (RestClientResponseException exception) {
            throw new OrderIntegrationException("Inventory service error: " + extractMessage(exception.getResponseBodyAsString(), exception.getStatusText()), exception);
        } catch (OrderIntegrationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new OrderIntegrationException("Unable to communicate with inventory service", exception);
        }
    }

    private void validateAdjustment(InventoryAdjustmentResponse response, String fallbackMessage) {
        if (response == null || !response.success()) {
            throw new OrderIntegrationException(response == null || response.message() == null || response.message().isBlank()
                    ? fallbackMessage
                    : response.message());
        }
    }

    private InventoryReservationRequest toRequest(Order order) {
        return new InventoryReservationRequest(
                order.getId(),
                order.getItems().stream()
                        .map(item -> new InventoryReservationItemRequest(
                                item.getProductId(),
                                item.getSize(),
                                item.getColor(),
                                item.getQuantity()
                        ))
                        .toList()
        );
    }

    private String buildReservationUrl() {
        return properties.getBaseUrl() + properties.getReservationPath();
    }

    private String buildConfirmUrl(Order order) {
        return properties.getBaseUrl() + properties.getConfirmPath().replace("{orderId}", order.getId().toString());
    }

    private String buildReleaseUrl(Order order) {
        return properties.getBaseUrl() + properties.getReleasePath().replace("{orderId}", order.getId().toString());
    }

    private String extractMessage(String body, String fallback) {
        if (body == null || body.isBlank()) {
            return fallback;
        }
        try {
            return objectMapper.readTree(body).path("message").asText(fallback);
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
