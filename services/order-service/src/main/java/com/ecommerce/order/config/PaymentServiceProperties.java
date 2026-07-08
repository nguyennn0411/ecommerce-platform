package com.ecommerce.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payment-service")
public class PaymentServiceProperties {

    private String baseUrl;
    private String createPayosPath;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getCreatePayosPath() {
        return createPayosPath;
    }

    public void setCreatePayosPath(String createPayosPath) {
        this.createPayosPath = createPayosPath;
    }
}
