package com.ecommerce.payment.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PayosPaymentLinkData(
        Long orderCode,
        Long amount,
        Long amountPaid,
        Long amountRemaining,
        String description,
        String checkoutUrl,
        String qrCode,
        String paymentLinkId,
        String status,
        String cancellationReason,
        String accountNumber,
        String reference,
        String transactionDateTime,
        String currency,
        String code,
        String desc,
        String createdAt,
        String canceledAt
) {
}
