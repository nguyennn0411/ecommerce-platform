package com.ecommerce.notification.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MailConfigurationResponse(
        boolean enabled,
        boolean configured,
        String host,
        Integer port,
        String username,
        String from,
        String fromName
) {
}
