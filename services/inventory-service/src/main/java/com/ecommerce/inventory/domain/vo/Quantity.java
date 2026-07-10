package com.ecommerce.inventory.domain.vo;

public record Quantity(int value) {

    public Quantity {
        if (value < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative" + value);
        }
    }

    public static Quantity of(int value) {
        return new Quantity(value);
    }

    public static Quantity zero() {
        return new Quantity(0);
    }
}