package com.ecommerce.user.application.dto;

public class AddressDTO {
    private int id;
    private String receiverName;
    private String receiverPhone;
    private String addressLine;
    private boolean isDefault;

    public AddressDTO() {
    }

    public AddressDTO(int id, String receiverName, String receiverPhone, String addressLine, boolean isDefault) {
        this.id = id;
        this.receiverName = receiverName;
        this.receiverPhone = receiverPhone;
        this.addressLine = addressLine;
        this.isDefault = isDefault;
    }

    public int getId() {
        return id;
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
