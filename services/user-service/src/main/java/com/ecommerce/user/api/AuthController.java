package com.ecommerce.user.api;

import com.ecommerce.user.application.GoogleAuthService;
import com.ecommerce.user.application.dto.AuthResponse;
import com.ecommerce.user.application.dto.GoogleLoginRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final GoogleAuthService googleAuthService;

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> googleLogin(
            @Valid @RequestBody GoogleLoginRequest request,
            HttpServletRequest servletRequest) {
        if (request.getIpAddress() == null || request.getIpAddress().isBlank()) {
            request.setIpAddress(servletRequest.getRemoteAddr());
        }
        if (request.getDeviceInfo() == null || request.getDeviceInfo().isBlank()) {
            request.setDeviceInfo(servletRequest.getHeader("User-Agent"));
        }
        AuthResponse response = googleAuthService.loginWithGoogle(request);
        return ResponseEntity.ok(response);
    }
}