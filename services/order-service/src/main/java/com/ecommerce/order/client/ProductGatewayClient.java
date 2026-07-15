package com.ecommerce.order.client;

import com.ecommerce.order.config.ProductServiceProperties;
import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.dto.ProductValidationItemRequest;
import com.ecommerce.order.dto.ProductValidationRequest;
import com.ecommerce.order.dto.ProductValidationResponse;
import com.ecommerce.order.exception.OrderIntegrationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

@Component
public class ProductGatewayClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ProductServiceProperties properties;

    public ProductGatewayClient(RestTemplate restTemplate,
                                ObjectMapper objectMapper,
                                ProductServiceProperties properties) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public void validateProducts(CreateOrderRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ProductValidationRequest> entity = new HttpEntity<>(toRequest(request), headers);

        try {
            ProductValidationResponse response = restTemplate.postForObject(buildValidationUrl(), entity, ProductValidationResponse.class);
            if (response == null || !response.valid()) {
                throw new OrderIntegrationException(response == null || response.message() == null || response.message().isBlank()
                        ? "Product validation failed"
                        : response.message());
            }
        } catch (RestClientResponseException exception) {
            throw new OrderIntegrationException("Product service error: " + extractMessage(exception.getResponseBodyAsString(), exception.getStatusText()), exception);
        } catch (OrderIntegrationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new OrderIntegrationException("Unable to communicate with product service", exception);
        }
    }

    private ProductValidationRequest toRequest(CreateOrderRequest request) {
        return new ProductValidationRequest(
                request.items().stream()
                        .map(item -> new ProductValidationItemRequest(
                                item.productId(),
                                item.unitPrice(),
                                item.quantity()
                        ))
                        .toList()
        );
    }

    private String buildValidationUrl() {
        return properties.getBaseUrl() + properties.getValidationPath();
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
