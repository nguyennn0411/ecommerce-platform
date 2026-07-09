package com.ecommerce.payment.util;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Component
public class PayosSignatureUtil {

    public String generatePaymentRequestSignature(long amount,
                                                  String cancelUrl,
                                                  String description,
                                                  long orderCode,
                                                  String returnUrl,
                                                  String checksumKey) {
        String rawData = "amount=%s&cancelUrl=%s&description=%s&orderCode=%s&returnUrl=%s"
                .formatted(amount, cancelUrl, description, orderCode, returnUrl);
        return hmacSha256(rawData, checksumKey);
    }

    public boolean verifyWebhookSignature(Map<String, ?> data,
                                          String signature,
                                          String checksumKey) {
        if (signature == null || signature.isBlank() || data == null) {
            return false;
        }
        String rawData = buildSortedData(data);
        String actualSignature = hmacSha256(rawData, checksumKey);
        return MessageDigest.isEqual(
                actualSignature.getBytes(StandardCharsets.UTF_8),
                signature.toLowerCase().getBytes(StandardCharsets.UTF_8)
        );
    }

    public String buildSortedData(Map<String, ?> data) {
        return new TreeMap<>(data).entrySet().stream()
                .map(entry -> entry.getKey() + "=" + Objects.toString(entry.getValue(), ""))
                .collect(Collectors.joining("&"));
    }

    public String hmacSha256(String rawData, String checksumKey) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(checksumKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(rawData.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).toLowerCase();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to create PayOS HMAC signature", exception);
        }
    }
}
