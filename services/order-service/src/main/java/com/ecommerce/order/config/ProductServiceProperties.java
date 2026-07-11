package com.ecommerce.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "product-service")
public class ProductServiceProperties {

    private String baseUrl;
    private String validationPath;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getValidationPath() {
        return validationPath;
    }

    public void setValidationPath(String validationPath) {
        this.validationPath = validationPath;
    }
}
