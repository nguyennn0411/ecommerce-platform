package com.ecommerce.notification.service;

public interface EmailSender {

    void send(String recipient, String subject, String content, boolean html);

    boolean isConfigured();
}
