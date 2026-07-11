package com.ecommerce.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "inventory-service")
public class InventoryServiceProperties {

    private String baseUrl;
    private String reservationPath;
    private String confirmPath;
    private String releasePath;

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

    public String getReleasePath() {
        return releasePath;
    }

    public void setReleasePath(String releasePath) {
        this.releasePath = releasePath;
    }

    public String getConfirmPath() {
        return confirmPath;
    }

    public void setConfirmPath(String confirmPath) {
        this.confirmPath = confirmPath;
    }
}
