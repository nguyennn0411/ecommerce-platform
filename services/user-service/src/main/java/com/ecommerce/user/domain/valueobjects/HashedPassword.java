package com.ecommerce.user.domain.valueobjects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public final class HashedPassword {

    @Column(name = "password_hash", nullable = false)
    private String hash;

    private HashedPassword(String hash) {
        this.hash = Objects.requireNonNull(hash, "Password hash cannot be null");
    }

    public static HashedPassword of(String hash) {
        if (hash.isBlank()) {
            throw new IllegalArgumentException("Password hash cannot be empty");
        }
        return new HashedPassword(hash);
    }

    @Override
    public String toString() {
        return "[PROTECTED HASH]";
    }
}