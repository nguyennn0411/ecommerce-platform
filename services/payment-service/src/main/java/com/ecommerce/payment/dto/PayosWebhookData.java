package com.ecommerce.payment.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.LinkedHashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PayosWebhookData(
        Long orderCode,
        Long amount,
        String description,
        String accountNumber,
        String reference,
        String transactionDateTime,
        String currency,
        String paymentLinkId,
        String code,
        String desc,
        String counterAccountBankId,
        String counterAccountBankName,
        String counterAccountName,
        String counterAccountNumber,
        String virtualAccountName,
        String virtualAccountNumber
) {
    public Map<String, Object> toSignatureMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("accountNumber", accountNumber);
        values.put("amount", amount);
        values.put("code", code);
        values.put("currency", currency);
        values.put("desc", desc);
        values.put("description", description);
        values.put("orderCode", orderCode);
        values.put("paymentLinkId", paymentLinkId);
        values.put("reference", reference);
        values.put("transactionDateTime", transactionDateTime);
        values.put("counterAccountBankId", counterAccountBankId);
        values.put("counterAccountBankName", counterAccountBankName);
        values.put("counterAccountName", counterAccountName);
        values.put("counterAccountNumber", counterAccountNumber);
        values.put("virtualAccountName", virtualAccountName);
        values.put("virtualAccountNumber", virtualAccountNumber);
        return values;
    }
}
