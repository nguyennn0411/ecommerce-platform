package com.ecommerce.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "inventory-service")
public class InventoryServiceProperties {

    private String baseUrl;
    private String reservationPath;
    private String confirmPathTemplate;
    private String releasePathTemplate;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getReservationPath() {
        return reservationPath;
    }

    public void setReservationPath(String reservationPath) {
        this.reservationPath = reservationPath;
    }

    public String getConfirmPathTemplate() {
        return confirmPathTemplate;
    }

    public void setConfirmPathTemplate(String confirmPathTemplate) {
        this.confirmPathTemplate = confirmPathTemplate;
    }

    public String getReleasePathTemplate() {
        return releasePathTemplate;
    }

    public void setReleasePathTemplate(String releasePathTemplate) {
        this.releasePathTemplate = releasePathTemplate;
    }
}
