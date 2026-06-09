package com.ecommerce.notification.service.impl;

import com.ecommerce.notification.config.NotificationMailProperties;
import com.ecommerce.notification.exception.MailConfigurationException;
import com.ecommerce.notification.exception.MailDeliveryException;
import com.ecommerce.notification.service.EmailSender;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

@Service
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender javaMailSender;
    private final MailProperties springMailProperties;
    private final NotificationMailProperties notificationMailProperties;

    public SmtpEmailSender(JavaMailSender javaMailSender,
                           MailProperties springMailProperties,
                           NotificationMailProperties notificationMailProperties) {
        this.javaMailSender = javaMailSender;
        this.springMailProperties = springMailProperties;
        this.notificationMailProperties = notificationMailProperties;
    }

    @Override
    public void send(String recipient, String subject, String content, boolean html) {
        validateConfiguration();
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, StandardCharsets.UTF_8.name());
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(content, html);
            helper.setFrom(fromAddress());
            if (hasText(notificationMailProperties.getReplyTo())) {
                helper.setReplyTo(notificationMailProperties.getReplyTo().trim());
            }
            javaMailSender.send(message);
        } catch (MailConfigurationException exception) {
            throw exception;
        } catch (MailException | jakarta.mail.MessagingException | UnsupportedEncodingException exception) {
            throw new MailDeliveryException("Unable to send email through SMTP", exception);
        }
    }

    @Override
    public boolean isConfigured() {
        return notificationMailProperties.isEnabled()
                && hasText(springMailProperties.getHost())
                && springMailProperties.getPort() != null
                && hasText(springMailProperties.getUsername())
                && hasText(springMailProperties.getPassword());
    }

    private void validateConfiguration() {
        if (!notificationMailProperties.isEnabled()) {
            throw new MailConfigurationException("Notification mail sending is disabled");
        }
        if (!isConfigured()) {
            throw new MailConfigurationException("SMTP mail is not configured. Set MAIL_USERNAME and MAIL_APP_PASSWORD or SPRING_MAIL_PASSWORD.");
        }
    }

    private InternetAddress fromAddress() throws UnsupportedEncodingException {
        String from = firstText(notificationMailProperties.getFrom(), springMailProperties.getUsername());
        String fromName = firstText(notificationMailProperties.getFromName(), "Ecommerce Store");
        return new InternetAddress(from, fromName, StandardCharsets.UTF_8.name());
    }

    private String firstText(String primary, String fallback) {
        return hasText(primary) ? primary.trim() : fallback;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
