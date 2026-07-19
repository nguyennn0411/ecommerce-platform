package com.ecommerce.payment.config;

import com.ecommerce.payment.service.PayosPaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.net.URI;

@Component
public class PayosWebhookRegistrar {

    private static final Logger log = LoggerFactory.getLogger(PayosWebhookRegistrar.class);

    private final PayosProperties payosProperties;
    private final PayosPaymentService payosPaymentService;

    public PayosWebhookRegistrar(PayosProperties payosProperties,
                                 PayosPaymentService payosPaymentService) {
        this.payosProperties = payosProperties;
        this.payosPaymentService = payosPaymentService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void confirmWebhookAtStartup() {
        String webhookUrl = payosProperties.getWebhookUrl();
        if (!payosProperties.isAutoConfirmWebhook() || !isPublicHttpsUrl(webhookUrl)) {
            log.warn("PayOS webhook was not confirmed. Set PAYOS_WEBHOOK_URL to a public HTTPS URL before accepting payments.");
            return;
        }

        try {
            var confirmation = payosPaymentService.confirmWebhook(webhookUrl);
            log.info("PayOS webhook confirmed: {}", confirmation.webhookUrl());
        } catch (RuntimeException exception) {
            log.error("Unable to confirm PayOS webhook {}. Payment service remains available, but orders will not update until the webhook is confirmed.", webhookUrl, exception);
        }
    }

    private boolean isPublicHttpsUrl(String value) {
        try {
            URI uri = URI.create(value);
            String host = uri.getHost();
            return "https".equalsIgnoreCase(uri.getScheme())
                    && host != null
                    && !host.equalsIgnoreCase("localhost")
                    && !host.equals("127.0.0.1")
                    && !host.equals("::1");
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
