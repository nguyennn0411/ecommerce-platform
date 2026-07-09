package com.ecommerce.user.domain.valueobjects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public final class DeviceSessionInfo {

    @Column(name = "device_info")
    private String deviceInfo;

    @Column(name = "ip_address", length = 100)
    private String ipAddress;

    public DeviceSessionInfo(String deviceInfo, String ipAddress) {
        this.deviceInfo = deviceInfo != null ? deviceInfo.trim() : "Unknown Device";
        this.ipAddress = ipAddress != null ? ipAddress.trim() : "Unknown IP";
    }

    @Override
    public String toString() {
        return String.format("Device: %s (IP: %s)", deviceInfo, ipAddress);
    }
}