package com.ecommerce.user.application.service;

import com.ecommerce.user.application.dto.AuthResponse;
import com.ecommerce.user.application.dto.GoogleLoginRequest;
import com.ecommerce.user.domain.model.aggregate.User;
import com.ecommerce.user.domain.repository.UserRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GoogleAuthService {

    private final UserRepository userRepository;

    @Value("${google.client.id}")
    private String googleClientId;

    @Transactional
    public AuthResponse loginWithGoogle(GoogleLoginRequest request) {
        GoogleIdToken.Payload payload = verifyGoogleToken(request.getIdToken());
        String email = payload.getEmail();
        Optional<User> userOptional = userRepository.findByEmail(email);
        User user;
        if (userOptional.isPresent()) {
            user = userOptional.get();
            if (!user.getStatus().equals("ACTIVE")) {
                throw new IllegalStateException("User account is not active");
            }
        } else {
            user = createNewGoogleUser(email);
        }
        userRepository.save(user);
        return AuthResponse.builder()
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }

    private GoogleIdToken.Payload verifyGoogleToken(String idTokenString) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    new GsonFactory()
            )
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();
            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                throw new IllegalArgumentException("Invalid Google Identity Token signature.");
            }
            return idToken.getPayload();
        } catch (GeneralSecurityException | IOException e) {
            throw new SecurityException("Failed to establish reliable connection to Google identity sub-system.", e);
        }
    }

    private User createNewGoogleUser(String email) {
        return User.builder().email(email).build();
    }

}