package com.ecommerce.order.client;

import com.ecommerce.order.config.InventoryServiceProperties;
import com.ecommerce.order.domain.Order;
import com.ecommerce.order.dto.InventoryAdjustmentResponse;
import com.ecommerce.order.dto.InventoryReservationItemRequest;
import com.ecommerce.order.dto.InventoryReservationRequest;
import com.ecommerce.order.dto.InventoryReservationResponse;
import com.ecommerce.order.exception.OrderIntegrationException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Component
public class InventoryGatewayClient {

    private final RestTemplate restTemplate;
    private final InventoryServiceProperties properties;

    public InventoryGatewayClient(RestTemplate restTemplate,
                                  InventoryServiceProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    public InventoryReservationResponse reserve(Order order) {
        InventoryReservationResponse response = postReservation(buildReservationUrl(), order);
        if (response == null || !response.reserved()) {
            throw new OrderIntegrationException(response == null
                    ? "Inventory service returned an empty reservation response"
                    : response.message());
        }
        return response;
    }

    public InventoryAdjustmentResponse confirm(UUID orderId) {
        InventoryAdjustmentResponse response = postAdjustment(buildConfirmUrl(orderId));
        if (response == null || !response.success()) {
            throw new OrderIntegrationException(response == null
                    ? "Inventory service returned an empty confirm response"
                    : response.message());
        }
        return response;
    }

    public InventoryAdjustmentResponse release(Order order) {
        return postAdjustment(buildReleaseUrl(order.getId()));
    }

    private InventoryReservationResponse postReservation(String url, Order order) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<InventoryReservationRequest> entity = new HttpEntity<>(toRequest(order), headers);

        try {
            return restTemplate.postForObject(url, entity, InventoryReservationResponse.class);
        } catch (RestClientResponseException exception) {
            throw new OrderIntegrationException("Inventory service error: " + exception.getStatusText(), exception);
        } catch (OrderIntegrationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new OrderIntegrationException("Unable to communicate with inventory service", exception);
        }
    }

    private InventoryAdjustmentResponse postAdjustment(String url) {
        try {
            return restTemplate.postForObject(url, null, InventoryAdjustmentResponse.class);
        } catch (RestClientResponseException exception) {
            throw new OrderIntegrationException("Inventory service error: " + exception.getStatusText(), exception);
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
                        .map(item -> new InventoryReservationItemRequest(item.getProductId(), item.getSize(), item.getColor(), item.getQuantity()))
                        .toList()
        );
    }

    private String buildReservationUrl() {
        return properties.getBaseUrl() + properties.getReservationPath();
    }

    private String buildConfirmUrl(UUID orderId) {
        return properties.getBaseUrl() + properties.getConfirmPathTemplate().formatted(orderId);
    }

    private String buildReleaseUrl(UUID orderId) {
        return properties.getBaseUrl() + properties.getReleasePathTemplate().formatted(orderId);
    }
}
