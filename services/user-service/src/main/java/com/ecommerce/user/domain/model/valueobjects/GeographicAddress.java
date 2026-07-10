package com.ecommerce.user.domain.model.valueobjects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public final class GeographicAddress {

    @Column(name = "address_line", nullable = false, length = 500)
    private String addressLine;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "district", length = 100)
    private String district;

    @Column(name = "ward", length = 100)
    private String ward;

    public GeographicAddress() {
    }

    public GeographicAddress(String addressLine, String city, String district, String ward) {
        this.addressLine = addressLine;
        this.city = city;
        this.district = district;
        this.ward = ward;
    }

    public GeographicAddress(String addressLine) {
        this.addressLine = addressLine;
    }

    public String getAddressLine() {
        return addressLine;
    }

    public String getCity() {
        return city;
    }

    public String getDistrict() {
        return district;
    }

    public String getWard() {
        return ward;
    }
}