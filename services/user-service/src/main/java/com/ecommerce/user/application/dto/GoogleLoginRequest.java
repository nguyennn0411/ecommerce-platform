package com.ecommerce.user.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GoogleLoginRequest {
    @NotBlank(message = "Id token is required")
    private String idToken;

    private String deviceInfo;

    private String ipAddress;
}