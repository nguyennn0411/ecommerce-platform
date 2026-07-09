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
public final class GeographicAddress {

    @Column(name = "address_line", nullable = false, length = 500)
    private String addressLine;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "district", nullable = false, length = 100)
    private String district;

    @Column(name = "ward", nullable = false, length = 100)
    private String ward;

    public GeographicAddress(String addressLine, String city, String district, String ward) {
        this.addressLine = validateNotBlank(addressLine, "Address line");
        this.city = validateNotBlank(city, "City");
        this.district = validateNotBlank(district, "District");
        this.ward = validateNotBlank(ward, "Ward");
    }

    private String validateNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or empty");
        }
        return value.trim();
    }

}