package com.ecommerce.order.integration;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "product-catalog-service", path = "/api/v1/products")
public interface ProductCatalogClient {

    @PostMapping("/validation")
    ProductValidationResponse validateProducts(@RequestBody ValidateProductsRequest request);
}
