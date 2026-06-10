package com.ecommerce.payment.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PayosWebhookRequest(
        String code,
        String desc,
        Boolean success,
        PayosWebhookData data,
        String signature
) {
}
