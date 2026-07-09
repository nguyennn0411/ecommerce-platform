package com.ecommerce.payment.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PayosCreatePaymentResponse(
        String code,
        String desc,
        PayosPaymentLinkData data,
        String signature
) {
}
