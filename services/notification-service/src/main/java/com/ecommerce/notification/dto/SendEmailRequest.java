package com.ecommerce.notification.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record SendEmailRequest(
        UUID userId,
        UUID orderId,
        @NotBlank @Email String recipient,
        @NotBlank @Size(max = 255) String subject,
        @NotBlank String content,
        boolean html
) {
}
