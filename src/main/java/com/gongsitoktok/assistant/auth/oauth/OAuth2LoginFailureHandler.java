/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/auth/oauth/OAuth2LoginFailureHandler.java
 */
package com.gongsitoktok.assistant.auth.oauth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * OAuth2 로그인 실패 핸들러.
 *
 * <p>{@code app.oauth.frontend-redirect-uri?error=OAUTH_FAILED&reason={code}} 로 302 redirect.</p>
 */
@Slf4j
@Component
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    @Value("${app.oauth.frontend-redirect-uri}")
    private String frontendRedirectUri;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        String reason = extractReason(exception);
        log.warn("OAuth2 로그인 실패: {} ({})", reason, exception.getMessage());
        String redirect = UriComponentsBuilder.fromUriString(frontendRedirectUri)
                .queryParam("error", "OAUTH_FAILED")
                .queryParam("reason", reason)
                .build()
                .toUriString();
        response.sendRedirect(redirect);
    }

    private String extractReason(AuthenticationException ex) {
        if (ex instanceof OAuth2AuthenticationException oauthEx && oauthEx.getError() != null) {
            return oauthEx.getError().getErrorCode();
        }
        return "AUTHENTICATION_FAILED";
    }
}
