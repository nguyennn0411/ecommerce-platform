package com.ecommerce.notification.api;

import com.ecommerce.common.web.ApiResponse;
import com.ecommerce.notification.dto.MailConfigurationResponse;
import com.ecommerce.notification.dto.NotificationResponse;
import com.ecommerce.notification.dto.SendEmailRequest;
import com.ecommerce.notification.service.NotificationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/email")
    public ApiResponse<NotificationResponse> sendEmail(@Valid @RequestBody SendEmailRequest request) {
        return ApiResponse.ok(notificationService.sendEmail(request), "Email sent successfully");
    }

    @GetMapping("/mail/config")
    public ApiResponse<MailConfigurationResponse> mailConfiguration() {
        return ApiResponse.ok(notificationService.getMailConfiguration());
    }

    @GetMapping("/{notificationId}")
    public ApiResponse<NotificationResponse> getById(@PathVariable UUID notificationId) {
        return ApiResponse.ok(notificationService.getNotification(notificationId));
    }

    @GetMapping
    public ApiResponse<List<NotificationResponse>> getByRecipient(@RequestParam @Email String recipient,
                                                                  @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.ok(notificationService.getByRecipient(recipient, limit));
    }

    @GetMapping("/orders/{orderId}")
    public ApiResponse<List<NotificationResponse>> getByOrderId(@PathVariable UUID orderId) {
        return ApiResponse.ok(notificationService.getByOrderId(orderId));
    }
}
