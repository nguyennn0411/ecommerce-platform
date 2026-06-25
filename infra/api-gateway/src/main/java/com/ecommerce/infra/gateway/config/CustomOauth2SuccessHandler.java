package com.ecommerce.infra.gateway.config;

import jakarta.annotation.Nonnull;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.net.URI;

@Component
public class CustomOauth2SuccessHandler implements ServerAuthenticationSuccessHandler {
    @Override
    public Mono<Void> onAuthenticationSuccess(WebFilterExchange webFilterExchange, Authentication authentication) {
        String internalJwtToken = getString(authentication);

        // 4. Redirect back to the Frontend Application with the Token
        // It's common to append it as a query param or set it as an HttpOnly Cookie
        String frontendRedirectUrl = "http://localhost:3000/oauth2/redirect?token=" + internalJwtToken;

        webFilterExchange.getExchange().getResponse().setStatusCode(org.springframework.http.HttpStatus.FOUND);
        webFilterExchange.getExchange().getResponse().getHeaders().setLocation(URI.create(frontendRedirectUrl));
        return webFilterExchange.getExchange().getResponse().setComplete();
    }

    @Nonnull
    private String getString(Authentication authentication) {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String picture = oAuth2User.getAttribute("picture");

        // 2. Business Logic Placeholder
        // Inside a real microservice, you would call your `user-service` via WebClient here:
        // userService.findOrCreateUser(email, name);

        return generateInternalJwt(email);
    }

    private String generateInternalJwt(String email) {
        // Implement your standard JWT building logic here (e.g., using io.jsonwebtoken:jjwt)
        // Set claims, subject (email/userId), expiration, and sign with your private/shared secret key
        return "mocked.jwt.token.here";
    }
}
