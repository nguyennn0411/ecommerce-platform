package com.ecommerce.infra.gateway.config;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.ecommerce.infra.gateway.dto.UserInternalDTO;
import com.ecommerce.infra.gateway.dto.UserSyncRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.util.Date;

@Component
public class CustomOauth2SuccessHandler implements ServerAuthenticationSuccessHandler {
    private final WebClient userServiceWebClient;

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration-ms}")
    private long expirationTimeMs;

    public CustomOauth2SuccessHandler(WebClient userServiceWebClient) {
        this.userServiceWebClient = userServiceWebClient;
    }

    @Override
    public Mono<Void> onAuthenticationSuccess(WebFilterExchange webFilterExchange, Authentication authentication) {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        if (email == null) {
            return Mono.error(new IllegalArgumentException("Email not found from OAuth2 provider"));
        }
        UserSyncRequest syncRequest = new UserSyncRequest(email);
        ServerWebExchange exchange = webFilterExchange.getExchange();
        return userServiceWebClient.post()
                .uri("/api/users/sync")
                .bodyValue(syncRequest)
                .retrieve()
                .bodyToMono(UserInternalDTO.class)
                .flatMap(internalUser -> {
                    String jwtToken = generateInternalJwt(internalUser);
                    System.out.println("JWT: " + jwtToken);
                    ResponseCookie cookie = ResponseCookie.from("INTERNAL_JWT", jwtToken)
                            .httpOnly(true)
                            .secure(false)
                            .path("/")
                            .maxAge(Duration.ofDays(1))
                            .build();
                    exchange.getResponse().addCookie(cookie);
                    exchange.getResponse().setStatusCode(HttpStatus.FOUND);
                    exchange.getResponse().getHeaders().setLocation(URI.create("http://localhost:3000/dashboard"));
                    return exchange.getResponse().setComplete();
                });
    }

    private String generateInternalJwt(UserInternalDTO internalDTO) {
        Algorithm algorithm = Algorithm.HMAC256(secretKey);
        return JWT.create()
                .withSubject(String.valueOf(internalDTO.getId()))
                .withClaim("email", internalDTO.getEmail())
                .withClaim("role", internalDTO.getRole())
                .withClaim("status", internalDTO.getStatus())
                .withClaim("emailVerified", internalDTO.isEmailVerified())
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + expirationTimeMs))
                .sign(algorithm);
    }
}
