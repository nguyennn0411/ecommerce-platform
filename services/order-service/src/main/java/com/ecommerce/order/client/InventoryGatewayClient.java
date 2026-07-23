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

    // Gửi yêu cầu giữ hàng tạm cho Inventory theo orderId và từng biến thể size/color.
    public void reserve(Order order) {
        // Gửi orderId + productId + size + color + quantity sang Inventory.
        InventoryReservationResponse response = reserveInventory(buildReservationUrl(), order);
        if (response == null || !response.reserved()) {
            throw new OrderIntegrationException(response == null || response.message() == null || response.message().isBlank()
                    ? "Inventory reservation failed"
                    : response.message());
        }
    }

    // Gửi yêu cầu xác nhận giữ hàng sau payment.success để Inventory trừ kho thật.
    public void confirm(Order order) {
        // Thanh toán OK: gọi Kho trừ tồn thật.
        InventoryAdjustmentResponse response = adjustInventory(buildConfirmUrl(order), null);
        validateAdjustment(response, "Inventory confirmation failed");
    }

    // Gửi yêu cầu trả hàng đã giữ khi payment fail, hủy hoặc đơn hết hạn thanh toán.
    public void release(Order order) {
        // Payment fail/hủy/quá hạn: trả phần hàng đã giữ.
        InventoryAdjustmentResponse response = adjustInventory(buildReleaseUrl(order), "payment-failed");
        validateAdjustment(response, "Inventory release failed");
    }

    // Thực hiện REST POST sang endpoint tạo reservation của Inventory.
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

    // Thực hiện REST POST sang endpoint confirm/release reservation của Inventory.
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

    // Kiểm tra kết quả confirm/release; Inventory báo fail thì Order dừng luồng tương ứng.
    private void validateAdjustment(InventoryAdjustmentResponse response, String fallbackMessage) {
        if (response == null || !response.success()) {
            throw new OrderIntegrationException(response == null || response.message() == null || response.message().isBlank()
                    ? fallbackMessage
                    : response.message());
        }
    }

    // Tạo payload gồm orderId và item productId/size/color/quantity gửi cho Inventory.
    private InventoryReservationRequest toRequest(Order order) {
        return new InventoryReservationRequest(
                order.getId(),  // Inventory dùng orderId để biết đơn nào giữ hàng
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

    // Endpoint giữ hàng lấy từ application.yml.
    private String buildReservationUrl() {
        return properties.getBaseUrl() + properties.getReservationPath();
    }

    // Endpoint xác nhận giữ hàng, thay {orderId} bằng id đơn hiện tại.
    private String buildConfirmUrl(Order order) {
        return properties.getBaseUrl() + properties.getConfirmPath().replace("{orderId}", order.getId().toString());
    }

    // Endpoint trả hàng đã giữ, thay {orderId} bằng id đơn hiện tại.
    private String buildReleaseUrl(Order order) {
        return properties.getBaseUrl() + properties.getReleasePath().replace("{orderId}", order.getId().toString());
    }

    // Rút message lỗi từ response Inventory để ghi vào đơn/Saga log.
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
