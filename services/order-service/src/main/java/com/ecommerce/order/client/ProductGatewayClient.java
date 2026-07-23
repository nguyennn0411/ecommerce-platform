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

    // Gọi Product Service để kiểm tra productId, giá hiện tại và số lượng trước khi tạo đơn.
    public void validateProducts(CreateOrderRequest request) {
        // Chuyển item FE gửi thành payload chỉ gồm productId, giá và số lượng để Product kiểm tra.
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ProductValidationRequest> entity = new HttpEntity<>(toRequest(request), headers);

        try {
            // Gọi Product trước khi tạo Order; Product là nguồn sự thật cho giá hiện tại.
            ProductValidationResponse response = restTemplate.postForObject(buildValidationUrl(), entity, ProductValidationResponse.class);
            if (response == null || !response.valid()) {
                // Giá bị sửa trên FE hoặc productId không hợp lệ thì dừng luồng đặt hàng tại đây.
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

    // Tạo payload validation; Order chỉ cần Product xác nhận danh sách item có hợp lệ không.
    private ProductValidationRequest toRequest(CreateOrderRequest request) {
        return new ProductValidationRequest(
                request.items().stream()
                        .map(item -> new ProductValidationItemRequest(
                                // Product không cần color/size để đối chiếu giá catalog hiện tại.
                                item.productId(),
                                item.unitPrice(),
                                item.quantity()
                        ))
                        .toList()
        );
    }

    // Ghép base URL và path validation lấy từ application.yml.
    private String buildValidationUrl() {
        // Ví dụ: http://product-catalog-service:8082/api/v1/products/validation.
        return properties.getBaseUrl() + properties.getValidationPath();
    }

    // Rút message lỗi từ response Product để trả về FE dễ hiểu hơn.
    private String extractMessage(String body, String fallback) {
        if (body == null || body.isBlank()) {
            return fallback;
        }
        try {
            // Lấy message Product trả về để FE hiển thị đúng lý do không đặt được đơn.
            return objectMapper.readTree(body).path("message").asText(fallback);
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
