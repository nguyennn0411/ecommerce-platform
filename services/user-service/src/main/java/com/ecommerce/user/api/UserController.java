package com.ecommerce.user.api;

import com.ecommerce.user.application.GoogleAuthService;
import com.ecommerce.user.application.UserService;
import com.ecommerce.user.application.dto.AuthResponse;
import com.ecommerce.user.application.dto.GoogleLoginRequest;
import com.ecommerce.user.application.dto.UserInternalDTO;
import com.ecommerce.user.application.dto.UserSyncRequest;
import com.ecommerce.user.domain.aggregate.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final GoogleAuthService googleAuthService;
    private final UserService userService;

    @PostMapping("/sync")
    public ResponseEntity<UserInternalDTO> sync(@RequestBody UserSyncRequest syncRequest) {
        User user = userService.findByEmail(syncRequest.getEmail());
        if (user == null) {
            user = userService.createNewUser(syncRequest.getEmail());
        }
        return ResponseEntity.ok(new UserInternalDTO(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.isEmailVerified()));
    }

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