package com.ecommerce.user.application.dto;

public class CreateAddressRequest {
    private String userId;
    private String receiverName;
    private String receiverPhone;
    private String addressLine;
    private boolean isDefault;

    public CreateAddressRequest() {
    }

    public CreateAddressRequest(String userId, String receiverName, String receiverPhone, String addressLine, boolean isDefault) {
        this.userId = userId;
        this.receiverName = receiverName;
        this.receiverPhone = receiverPhone;
        this.addressLine = addressLine;
        this.isDefault = isDefault;
    }

    public String getUserId() {
        return userId;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public String getReceiverPhone() {
        return receiverPhone;
    }

    public String getAddressLine() {
        return addressLine;
    }

    public boolean isDefault() {
        return isDefault;
    }
}
