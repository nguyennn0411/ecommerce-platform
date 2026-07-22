package com.ecommerce.payment.service.impl;

import com.ecommerce.payment.config.PayosProperties;
import com.ecommerce.payment.dto.CancelPaymentRequest;
import com.ecommerce.payment.dto.CreatePayosPaymentRequest;
import com.ecommerce.payment.dto.CreatePayosPaymentResponse;
import com.ecommerce.payment.dto.PayosCreatePaymentRequest;
import com.ecommerce.payment.dto.PayosCreatePaymentResponse;
import com.ecommerce.payment.dto.PayosPaymentLinkData;
import com.ecommerce.payment.dto.PayosWebhookData;
import com.ecommerce.payment.dto.PayosWebhookRequest;
import com.ecommerce.payment.dto.PaymentResponse;
import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.enums.PaymentStatus;
import com.ecommerce.payment.enums.TransactionType;
import com.ecommerce.payment.exception.GatewayException;
import com.ecommerce.payment.exception.InvalidPaymentSignatureException;
import com.ecommerce.payment.exception.PaymentAlreadyProcessedException;
import com.ecommerce.payment.exception.PaymentNotFoundException;
import com.ecommerce.payment.repository.PaymentRepository;
import com.ecommerce.payment.service.PaymentEventPublisher;
import com.ecommerce.payment.service.PaymentService;
import com.ecommerce.payment.service.PaymentTransactionService;
import com.ecommerce.payment.service.PayosPaymentService;
import com.ecommerce.payment.util.PayosSignatureUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class PaymentServiceImpl implements PaymentService {

    private static final String VND = "VND";

    private final PaymentRepository paymentRepository;
    private final PayosPaymentService payosPaymentService;
    private final PaymentTransactionService paymentTransactionService;
    private final PaymentEventPublisher paymentEventPublisher;
    private final PayosProperties payosProperties;
    private final PayosSignatureUtil signatureUtil;
    private final ObjectMapper objectMapper;

    public PaymentServiceImpl(PaymentRepository paymentRepository,
                              PayosPaymentService payosPaymentService,
                              PaymentTransactionService paymentTransactionService,
                              PaymentEventPublisher paymentEventPublisher,
                              PayosProperties payosProperties,
                              PayosSignatureUtil signatureUtil,
                              ObjectMapper objectMapper) {
        this.paymentRepository = paymentRepository;
        this.payosPaymentService = payosPaymentService;
        this.paymentTransactionService = paymentTransactionService;
        this.paymentEventPublisher = paymentEventPublisher;
        this.payosProperties = payosProperties;
        this.signatureUtil = signatureUtil;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(noRollbackFor = GatewayException.class)
    public CreatePayosPaymentResponse createPayment(CreatePayosPaymentRequest request) {
        String currency = normalizeCurrency(request.currency());
        requireVnd(currency);

        Payment existingPayment = paymentRepository.findByOrderId(request.orderId()).orElse(null);
        if (existingPayment != null) {
            if (!existingPayment.isPending()) {
                throw new PaymentAlreadyProcessedException(request.orderId());
            }
            if (hasCheckoutInfo(existingPayment)) {
                return toCreateResponse(existingPayment, null, "Existing pending payment returned");
            }
            requirePayosCredentials();
            try {
                PayosCreatePaymentResponse statusResponse = payosPaymentService.getPaymentStatus(existingPayment.getOrderCode());
                if (statusResponse != null && statusResponse.data() != null) {
                    Payment synced = applyCheckoutInfo(existingPayment, statusResponse.data());
                    return toCreateResponse(synced, statusResponse, "Existing pending payment returned");
                }
            } catch (GatewayException ignored) {
                // fall through and return the existing pending payment data
            }
            return toCreateResponse(existingPayment, null, "Existing pending payment returned");
        }

        requirePayosCredentials();

        Payment payment = new Payment(
                request.orderId(),
                request.userId(),
                nextOrderCode(),
                request.amount(),
                currency,
                request.buyerName(),
                request.buyerEmail()
        );
        payment = paymentRepository.save(payment);

        String description = normalizeDescription(request.description(), payment.getOrderCode());
        PayosCreatePaymentRequest gatewayRequest = buildGatewayRequest(request, payment, description);

        PayosCreatePaymentResponse gatewayResponse;
        try {
            gatewayResponse = payosPaymentService.createPayment(gatewayRequest);
        } catch (GatewayException exception) {
            markPaymentFailed(payment, exception.getMessage(), payment.getPaymentLinkId(), exception.getMessage());
            throw exception;
        }

        if (!isSuccessCode(gatewayResponse.code()) || gatewayResponse.data() == null) {
            String reason = resolveGatewayReason(gatewayResponse, "PayOS create payment rejected the request");
            markPaymentFailed(payment, reason, providerReference(gatewayResponse), toJson(gatewayResponse));
            throw new GatewayException(reason);
        }

        Payment updatedPayment = applyCheckoutInfo(payment, gatewayResponse.data());
        paymentTransactionService.record(
                updatedPayment,
                TransactionType.PAYMENT,
                PaymentStatus.PENDING,
                gatewayResponse.data().paymentLinkId(),
                toJson(gatewayResponse)
        );
        paymentEventPublisher.publishPaymentCreated(updatedPayment);
        return toCreateResponse(updatedPayment, gatewayResponse, "PayOS payment created successfully");
    }

    @Override
    @Transactional
    public Map<String, Object> handleWebhook(PayosWebhookRequest request) {
        if (request == null || request.data() == null) {
            throw new IllegalArgumentException("Webhook payload is missing data");
        }
        requirePayosCredentials();
        if (!payosPaymentService.verifyWebhookSignature(request)) {
            throw new InvalidPaymentSignatureException();
        }

        PayosWebhookData data = request.data();
        if (data.orderCode() == null) {
            throw new IllegalArgumentException("Webhook orderCode is required");
        }
        Payment payment = paymentRepository.findByOrderCodeForUpdate(data.orderCode()).orElse(null);
        if (payment == null) {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("received", true);
            response.put("orderCode", data.orderCode());
            response.put("status", "IGNORED");
            response.put("message", "Webhook acknowledged for an unknown payment");
            return response;
        }

        validateWebhookPayload(payment, data);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("received", true);
        response.put("orderCode", payment.getOrderCode());
        response.put("paymentId", payment.getId());

        if (payment.isFinalized()) {
            response.put("status", payment.getStatus().name());
            response.put("message", "Webhook ignored because payment is already finalized");
            return response;
        }

        String providerReference = Objects.toString(data.reference(), payment.getPaymentLinkId());
        String providerResponse = toJson(request);

        if (isSuccessfulWebhook(request, data)) {
            payment.markSuccess();
            paymentTransactionService.record(
                    payment,
                    TransactionType.PAYMENT,
                    PaymentStatus.SUCCESS,
                    providerReference,
                    providerResponse
            );
            paymentEventPublisher.publishPaymentSuccess(payment, providerReference);
            response.put("status", payment.getStatus().name());
            response.put("message", "Payment completed");
            return response;
        }

        if (isCancelledWebhook(request, data)) {
            String reason = resolveReason(request.desc(), data.desc(), "Cancelled by PayOS");
            payment.markCancelled(reason);
            paymentTransactionService.record(
                    payment,
                    TransactionType.CANCEL,
                    PaymentStatus.CANCELLED,
                    providerReference,
                    providerResponse
            );
            paymentEventPublisher.publishPaymentCancelled(payment, reason);
            response.put("status", payment.getStatus().name());
            response.put("message", reason);
            return response;
        }

        String reason = resolveReason(request.desc(), data.desc(), "Payment failed");
        payment.markFailed(reason);
        paymentTransactionService.record(
                payment,
                TransactionType.PAYMENT,
                PaymentStatus.FAILED,
                providerReference,
                providerResponse
        );
        paymentEventPublisher.publishPaymentFailed(payment, reason);
        response.put("status", payment.getStatus().name());
        response.put("message", reason);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> handleReturn(Map<String, String> params) {
        return buildRedirectResponse("PayOS return URL is display-only. Webhook is the source of truth.", params);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> handleCancel(Map<String, String> params) {
        return buildRedirectResponse("PayOS cancel URL is display-only. Webhook is the source of truth.", params);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrderId(UUID orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> PaymentNotFoundException.byOrderId(orderId));
        return PaymentResponse.from(payment);
    }

    @Override
    @Transactional
    public PaymentResponse syncPaymentByOrderId(UUID orderId) {
        Payment payment = paymentRepository.findByOrderIdForUpdate(orderId)
                .orElseThrow(() -> PaymentNotFoundException.byOrderId(orderId));
        if (!payment.isPending()) {
            return PaymentResponse.from(payment);
        }

        requirePayosCredentials();
        PayosCreatePaymentResponse providerResponse = payosPaymentService.getPaymentStatus(payment.getOrderCode());
        if (providerResponse == null || providerResponse.data() == null || !isSuccessCode(providerResponse.code())) {
            throw new GatewayException(resolveGatewayReason(providerResponse, "Unable to reconcile payment status with PayOS"));
        }

        PayosPaymentLinkData providerPayment = providerResponse.data();
        validateProviderPayment(payment, providerPayment);
        applyCheckoutInfo(payment, providerPayment);

        String providerStatus = Objects.toString(providerPayment.status(), "").trim().toUpperCase(Locale.ROOT);
        String providerReference = Objects.toString(providerPayment.reference(), payment.getPaymentLinkId());
        if ("PAID".equals(providerStatus)) {
            payment.markSuccess();
            paymentTransactionService.record(payment, TransactionType.PAYMENT, PaymentStatus.SUCCESS,
                    providerReference, toJson(providerResponse));
            paymentEventPublisher.publishPaymentSuccess(payment, providerReference);
        } else if (providerStatus.contains("CANCEL")) {
            String reason = resolveReason(providerPayment.cancellationReason(), providerPayment.desc(), "Cancelled by PayOS");
            payment.markCancelled(reason);
            paymentTransactionService.record(payment, TransactionType.CANCEL, PaymentStatus.CANCELLED,
                    providerReference, toJson(providerResponse));
            paymentEventPublisher.publishPaymentCancelled(payment, reason);
        }

        return PaymentResponse.from(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> PaymentNotFoundException.byPaymentId(paymentId));
        return PaymentResponse.from(payment);
    }

    @Override
    @Transactional
    public PaymentResponse cancelPayment(UUID paymentId, CancelPaymentRequest request) {
        requirePayosCredentials();
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> PaymentNotFoundException.byPaymentId(paymentId));

        if (payment.getStatus() == PaymentStatus.SUCCESS || payment.getStatus() == PaymentStatus.REFUNDED) {
            throw new PaymentAlreadyProcessedException(payment.getOrderId());
        }
        if (payment.getStatus() == PaymentStatus.CANCELLED) {
            return PaymentResponse.from(payment);
        }

        String reason = resolveReason(request == null ? null : request.reason(), null, "Cancelled by request");
        PayosCreatePaymentResponse gatewayResponse = payosPaymentService.cancelPayment(payment.getOrderCode(), reason);
        if (!isSuccessCode(gatewayResponse.code())) {
            throw new GatewayException(resolveGatewayReason(gatewayResponse, "PayOS cancel payment rejected the request"));
        }

        String providerReference = providerReference(gatewayResponse);
        payment.markCancelled(reason);
        paymentTransactionService.record(
                payment,
                TransactionType.CANCEL,
                PaymentStatus.CANCELLED,
                providerReference,
                toJson(gatewayResponse)
        );
        paymentEventPublisher.publishPaymentCancelled(payment, reason);
        return PaymentResponse.from(payment);
    }

    private Payment applyCheckoutInfo(Payment payment, PayosPaymentLinkData data) {
        payment.attachCheckoutInfo(data.paymentLinkId(), data.checkoutUrl(), data.qrCode());
        return paymentRepository.save(payment);
    }

    private PayosCreatePaymentRequest buildGatewayRequest(CreatePayosPaymentRequest request,
                                                          Payment payment,
                                                          String description) {
        long amount = toWholeAmount(request.amount());
        String cancelUrl = appendOrderId(payosProperties.getCancelUrl(), payment.getOrderId());
        String returnUrl = appendOrderId(payosProperties.getReturnUrl(), payment.getOrderId());
        String signature = signatureUtil.generatePaymentRequestSignature(
                amount,
                cancelUrl,
                description,
                payment.getOrderCode(),
                returnUrl,
                payosProperties.getChecksumKey()
        );
        return new PayosCreatePaymentRequest(
                payment.getOrderCode(),
                amount,
                description,
                request.buyerName(),
                emptyToNull(request.buyerEmail()),
                emptyToNull(request.buyerPhone()),
                emptyToNull(request.buyerAddress()),
                cancelUrl,
                returnUrl,
                signature
        );
    }

    private String appendOrderId(String baseUrl, UUID orderId) {
        String separator = baseUrl.contains("?") ? "&" : "?";
        return baseUrl + separator + "orderId=" + URLEncoder.encode(orderId.toString(), StandardCharsets.UTF_8);
    }

    private CreatePayosPaymentResponse toCreateResponse(Payment payment,
                                                        PayosCreatePaymentResponse gatewayResponse,
                                                        String message) {
        PayosPaymentLinkData data = gatewayResponse == null ? null : gatewayResponse.data();
        return new CreatePayosPaymentResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getOrderCode(),
                payment.getBuyerName(),
                payment.getBuyerEmail(),
                payment.getStatus(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getPaymentMethod(),
                payment.getPaymentProvider(),
                data == null ? payment.getPaymentLinkId() : data.paymentLinkId(),
                data == null ? payment.getCheckoutUrl() : data.checkoutUrl(),
                data == null ? payment.getQrCode() : data.qrCode(),
                message
        );
    }

    private Map<String, Object> buildRedirectResponse(String message, Map<String, String> params) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", message);
        response.put("queryParams", params == null ? Map.of() : new LinkedHashMap<>(params));

        Long orderCode = parseOrderCode(params == null ? null : params.get("orderCode"));
        if (orderCode != null) {
            paymentRepository.findByOrderCode(orderCode).ifPresent(payment -> {
                response.put("paymentId", payment.getId());
                response.put("orderId", payment.getOrderId());
                response.put("orderCode", payment.getOrderCode());
                response.put("status", payment.getStatus().name());
                response.put("paymentLinkId", payment.getPaymentLinkId());
                response.put("checkoutUrl", payment.getCheckoutUrl());
            });
        }
        return response;
    }

    private void validateWebhookPayload(Payment payment, PayosWebhookData data) {
        if (data.amount() != null && !Objects.equals(toWholeAmount(payment.getAmount()), data.amount())) {
            throw new GatewayException("PayOS amount does not match payment amount");
        }
        if (data.currency() != null
                && payment.getCurrency() != null
                && !payment.getCurrency().equalsIgnoreCase(data.currency())) {
            throw new GatewayException("PayOS currency does not match payment currency");
        }
        if (data.paymentLinkId() != null
                && payment.getPaymentLinkId() != null
                && !Objects.equals(payment.getPaymentLinkId(), data.paymentLinkId())) {
            throw new GatewayException("PayOS paymentLinkId does not match payment");
        }
    }

    private void validateProviderPayment(Payment payment, PayosPaymentLinkData data) {
        if (data.orderCode() != null && !Objects.equals(payment.getOrderCode(), data.orderCode())) {
            throw new GatewayException("PayOS orderCode does not match payment");
        }
        if (data.amount() != null && !Objects.equals(toWholeAmount(payment.getAmount()), data.amount())) {
            throw new GatewayException("PayOS amount does not match payment amount");
        }
        if (data.paymentLinkId() != null && payment.getPaymentLinkId() != null
                && !Objects.equals(payment.getPaymentLinkId(), data.paymentLinkId())) {
            throw new GatewayException("PayOS paymentLinkId does not match payment");
        }
    }

    private boolean isSuccessfulWebhook(PayosWebhookRequest request, PayosWebhookData data) {
        return Boolean.TRUE.equals(request.success())
                || isSuccessCode(request.code())
                || isSuccessCode(data.code());
    }

    private boolean isCancelledWebhook(PayosWebhookRequest request, PayosWebhookData data) {
        String combined = (Objects.toString(request.desc(), "") + " "
                + Objects.toString(data.desc(), "") + " "
                + Objects.toString(request.code(), "") + " "
                + Objects.toString(data.code(), "")).toLowerCase(Locale.ROOT);
        return combined.contains("cancel");
    }

    private String resolveGatewayReason(PayosCreatePaymentResponse response, String fallback) {
        if (response == null) {
            return fallback;
        }
        if (response.desc() != null && !response.desc().isBlank()) {
            return response.desc();
        }
        if (response.data() != null) {
            if (response.data().desc() != null && !response.data().desc().isBlank()) {
                return response.data().desc();
            }
            if (response.data().cancellationReason() != null && !response.data().cancellationReason().isBlank()) {
                return response.data().cancellationReason();
            }
        }
        return fallback;
    }

    private String resolveReason(String primary, String secondary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        if (secondary != null && !secondary.isBlank()) {
            return secondary;
        }
        return fallback;
    }

    private Long parseOrderCode(String orderCode) {
        if (orderCode == null || orderCode.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(orderCode);
        } catch (NumberFormatException exception) {
            throw new GatewayException("Invalid orderCode returned in PayOS redirect");
        }
    }

    private long nextOrderCode() {
        long candidate = Instant.now().toEpochMilli();
        while (paymentRepository.existsByOrderCode(candidate)) {
            candidate++;
        }
        return candidate;
    }

    private long toWholeAmount(BigDecimal amount) {
        try {
            return amount.stripTrailingZeros().longValueExact();
        } catch (ArithmeticException exception) {
            throw new GatewayException("Payment amount must be a whole VND amount", exception);
        }
    }

    private void requireVnd(String currency) {
        if (currency == null || !VND.equalsIgnoreCase(currency)) {
            throw new IllegalArgumentException("Only VND currency is supported for PayOS payments");
        }
    }

    private String normalizeCurrency(String currency) {
        return currency == null || currency.isBlank() ? VND : currency.toUpperCase(Locale.ROOT);
    }

    private String normalizeDescription(String description, Long orderCode) {
        String sanitized = Normalizer.normalize(Objects.toString(description, ""), Normalizer.Form.NFKC)
                .replaceAll("\\s+", " ")
                .trim();
        if (sanitized.isBlank()) {
            sanitized = "DH" + orderCode;
        }
        if (sanitized.length() > 25) {
            sanitized = sanitized.substring(0, 25);
        }
        return sanitized;
    }

    private String providerReference(PayosCreatePaymentResponse response) {
        if (response == null || response.data() == null) {
            return null;
        }
        if (response.data().reference() != null && !response.data().reference().isBlank()) {
            return response.data().reference();
        }
        return response.data().paymentLinkId();
    }

    private void markPaymentFailed(Payment payment,
                                   String reason,
                                   String providerReference,
                                   String providerResponse) {
        if (!payment.isPending()) {
            return;
        }
        payment.markFailed(reason);
        paymentTransactionService.record(
                payment,
                TransactionType.PAYMENT,
                PaymentStatus.FAILED,
                providerReference,
                providerResponse
        );
        paymentEventPublisher.publishPaymentFailed(payment, reason);
    }

    private boolean isSuccessCode(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }
        String normalized = code.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("0")
                || normalized.equals("00")
                || normalized.equals("ok")
                || normalized.equals("success");
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private boolean hasCheckoutInfo(Payment payment) {
        return payment.getPaymentLinkId() != null
                || payment.getCheckoutUrl() != null
                || payment.getQrCode() != null;
    }

    private void requirePayosCredentials() {
        if (!payosProperties.hasCredentials()) {
            throw new GatewayException("PayOS credentials are not configured. Set PAYOS_CLIENT_ID, PAYOS_API_KEY, and PAYOS_CHECKSUM_KEY.");
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new GatewayException("Unable to serialize PayOS response", exception);
        }
    }
}
