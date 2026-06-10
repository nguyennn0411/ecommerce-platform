package com.ecommerce.notification.service.impl;

import com.ecommerce.common.events.PaymentCancelledEvent;
import com.ecommerce.common.events.PaymentCreatedEvent;
import com.ecommerce.common.events.PaymentFailedEvent;
import com.ecommerce.common.events.PaymentRefundedEvent;
import com.ecommerce.common.events.PaymentSuccessEvent;
import com.ecommerce.notification.config.NotificationMailProperties;
import com.ecommerce.notification.dto.MailConfigurationResponse;
import com.ecommerce.notification.dto.NotificationResponse;
import com.ecommerce.notification.dto.SendEmailRequest;
import com.ecommerce.notification.entity.Notification;
import com.ecommerce.notification.enums.NotificationChannel;
import com.ecommerce.notification.exception.NotificationNotFoundException;
import com.ecommerce.notification.repository.NotificationRepository;
import com.ecommerce.notification.service.EmailSender;
import com.ecommerce.notification.service.NotificationService;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final int MAX_LIST_LIMIT = 100;

    private final NotificationRepository notificationRepository;
    private final EmailSender emailSender;
    private final MailProperties springMailProperties;
    private final NotificationMailProperties notificationMailProperties;

    public NotificationServiceImpl(NotificationRepository notificationRepository,
                                   EmailSender emailSender,
                                   MailProperties springMailProperties,
                                   NotificationMailProperties notificationMailProperties) {
        this.notificationRepository = notificationRepository;
        this.emailSender = emailSender;
        this.springMailProperties = springMailProperties;
        this.notificationMailProperties = notificationMailProperties;
    }

    @Override
    @Transactional
    public NotificationResponse sendEmail(SendEmailRequest request) {
        Notification notification = sendEmail(
                request.userId(),
                request.orderId(),
                request.recipient(),
                request.subject(),
                request.content(),
                request.html()
        );
        return NotificationResponse.from(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationResponse getNotification(UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));
        return NotificationResponse.from(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getByRecipient(String recipient, int limit) {
        int boundedLimit = Math.max(1, Math.min(limit, MAX_LIST_LIMIT));
        return notificationRepository
                .findByRecipientIgnoreCaseOrderByCreatedAtDesc(recipient, PageRequest.of(0, boundedLimit))
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getByOrderId(UUID orderId) {
        return notificationRepository.findByOrderIdOrderByCreatedAtDesc(orderId).stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @Override
    public MailConfigurationResponse getMailConfiguration() {
        return new MailConfigurationResponse(
                notificationMailProperties.isEnabled(),
                emailSender.isConfigured(),
                springMailProperties.getHost(),
                springMailProperties.getPort(),
                springMailProperties.getUsername(),
                firstText(notificationMailProperties.getFrom(), springMailProperties.getUsername()),
                firstText(notificationMailProperties.getFromName(), "Ecommerce Store")
        );
    }

    @Override
    @Transactional
    public void sendPaymentCreated(PaymentCreatedEvent event) {
        if (!hasText(event.buyerEmail())) {
            return;
        }
        String subject = "Payment link for order " + event.orderCode();
        String body = paymentCreatedBody(event);
        sendEmail(event.userId(), event.orderId(), event.buyerEmail(), subject, body, true);
    }

    @Override
    @Transactional
    public void sendPaymentSuccess(PaymentSuccessEvent event) {
        if (!hasText(event.buyerEmail())) {
            return;
        }
        String subject = "Payment received for order " + event.orderCode();
        String body = paymentStatusBody(
                "Payment received",
                event.buyerName(),
                event.orderCode(),
                event.amount(),
                event.currency(),
                "Your payment has been confirmed successfully.",
                null
        );
        sendEmail(event.userId(), event.orderId(), event.buyerEmail(), subject, body, true);
    }

    @Override
    @Transactional
    public void sendPaymentFailed(PaymentFailedEvent event) {
        if (!hasText(event.buyerEmail())) {
            return;
        }
        String subject = "Payment failed for order " + event.orderCode();
        String body = paymentStatusBody(
                "Payment failed",
                event.buyerName(),
                event.orderCode(),
                event.amount(),
                event.currency(),
                "We could not complete your payment.",
                event.reason()
        );
        sendEmail(event.userId(), event.orderId(), event.buyerEmail(), subject, body, true);
    }

    @Override
    @Transactional
    public void sendPaymentCancelled(PaymentCancelledEvent event) {
        if (!hasText(event.buyerEmail())) {
            return;
        }
        String subject = "Payment cancelled for order " + event.orderCode();
        String body = paymentStatusBody(
                "Payment cancelled",
                event.buyerName(),
                event.orderCode(),
                event.amount(),
                event.currency(),
                "The payment link has been cancelled.",
                event.reason()
        );
        sendEmail(event.userId(), event.orderId(), event.buyerEmail(), subject, body, true);
    }

    @Override
    @Transactional
    public void sendPaymentRefunded(PaymentRefundedEvent event) {
        if (!hasText(event.buyerEmail())) {
            return;
        }
        String subject = "Payment refunded for order " + event.orderCode();
        String body = paymentStatusBody(
                "Payment refunded",
                event.buyerName(),
                event.orderCode(),
                event.amount(),
                event.currency(),
                "Your payment has been refunded.",
                event.providerReference()
        );
        sendEmail(event.userId(), event.orderId(), event.buyerEmail(), subject, body, true);
    }

    private Notification sendEmail(UUID userId,
                                   UUID orderId,
                                   String recipient,
                                   String subject,
                                   String content,
                                   boolean html) {
        Notification notification = new Notification(
                userId,
                orderId,
                NotificationChannel.EMAIL,
                recipient,
                subject,
                content
        );
        notification = notificationRepository.save(notification);

        try {
            emailSender.send(recipient, subject, content, html);
            notification.markSent();
            return notificationRepository.save(notification);
        } catch (RuntimeException exception) {
            notification.markFailed(rootMessage(exception));
            notificationRepository.save(notification);
            throw exception;
        }
    }

    private String paymentCreatedBody(PaymentCreatedEvent event) {
        String checkoutBlock = hasText(event.checkoutUrl())
                ? """
                <p>
                    <a href="%s" style="display:inline-block;padding:12px 18px;background:#111827;color:#ffffff;text-decoration:none;border-radius:6px;">
                        Open payment link
                    </a>
                </p>
                <p style="word-break:break-all;color:#374151;">%s</p>
                """.formatted(escapeAttribute(event.checkoutUrl()), escape(event.checkoutUrl()))
                : "<p>Your payment link is being prepared. Please check your order again shortly.</p>";

        return htmlShell(
                "Payment link is ready",
                """
                <p>Hello %s,</p>
                <p>Your payment link for order <strong>%s</strong> has been created.</p>
                %s
                %s
                """.formatted(
                        safeName(event.buyerName()),
                        escape(String.valueOf(event.orderCode())),
                        detailTable(event.amount(), event.currency(), event.paymentLinkId(), event.paymentProvider()),
                        checkoutBlock
                )
        );
    }

    private String paymentStatusBody(String title,
                                     String buyerName,
                                     Long orderCode,
                                     BigDecimal amount,
                                     String currency,
                                     String message,
                                     String detail) {
        String detailLine = hasText(detail)
                ? "<p><strong>Detail:</strong> %s</p>".formatted(escape(detail))
                : "";
        return htmlShell(
                title,
                """
                <p>Hello %s,</p>
                <p>%s</p>
                <p>Order: <strong>%s</strong></p>
                <p>Amount: <strong>%s</strong></p>
                %s
                """.formatted(
                        safeName(buyerName),
                        escape(message),
                        escape(String.valueOf(orderCode)),
                        escape(money(amount, currency)),
                        detailLine
                )
        );
    }

    private String detailTable(BigDecimal amount, String currency, String paymentLinkId, String provider) {
        return """
                <table style="border-collapse:collapse;margin:16px 0;width:100%%;">
                    <tr><td style="padding:8px;border:1px solid #e5e7eb;">Amount</td><td style="padding:8px;border:1px solid #e5e7eb;"><strong>%s</strong></td></tr>
                    <tr><td style="padding:8px;border:1px solid #e5e7eb;">Provider</td><td style="padding:8px;border:1px solid #e5e7eb;">%s</td></tr>
                    <tr><td style="padding:8px;border:1px solid #e5e7eb;">Payment link ID</td><td style="padding:8px;border:1px solid #e5e7eb;word-break:break-all;">%s</td></tr>
                </table>
                """.formatted(
                escape(money(amount, currency)),
                escape(provider),
                escape(firstText(paymentLinkId, "N/A"))
        );
    }

    private String htmlShell(String title, String body) {
        return """
                <!doctype html>
                <html>
                <body style="font-family:Arial,sans-serif;line-height:1.55;color:#111827;">
                    <div style="max-width:640px;margin:0 auto;padding:24px;">
                        <h2 style="margin:0 0 16px;">%s</h2>
                        %s
                        <p style="margin-top:24px;color:#6b7280;font-size:13px;">This is an automated email from Ecommerce Store.</p>
                    </div>
                </body>
                </html>
                """.formatted(escape(title), body);
    }

    private String money(BigDecimal amount, String currency) {
        if (amount == null) {
            return "N/A";
        }
        return amount.stripTrailingZeros().toPlainString() + " " + firstText(currency, "VND");
    }

    private String safeName(String name) {
        return escape(firstText(name, "customer"));
    }

    private String escape(String value) {
        return HtmlUtils.htmlEscape(firstText(value, ""));
    }

    private String escapeAttribute(String value) {
        return HtmlUtils.htmlEscape(firstText(value, ""));
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return firstText(current.getMessage(), throwable.getMessage());
    }

    private String firstText(String primary, String fallback) {
        return hasText(primary) ? primary.trim() : fallback;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
