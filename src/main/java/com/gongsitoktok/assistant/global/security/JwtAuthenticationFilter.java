/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/global/security/JwtAuthenticationFilter.java
 */
package com.gongsitoktok.assistant.global.security;

import com.gongsitoktok.assistant.auth.jwt.JwtTokenProvider;
import com.gongsitoktok.assistant.global.error.exception.BusinessException;
import com.gongsitoktok.assistant.user.entity.OauthService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT 인증 필터.
 *
 * <h3>동작 순서 (제작요청 v6 §스텝 F · Spring_챗봇_핸들링_가이드 §6 정합)</h3>
 * <ol>
 *     <li>{@code Authorization: Bearer ...} 헤더 추출. 없으면 인증 없이 다음 필터로 통과 (permitAll 경로 처리 위함).</li>
 *     <li>{@link JwtTokenProvider#parseAndValidate(String)} 호출 — 내부적으로 블랙리스트 → 캐시 → 서명 파싱 순서.</li>
 *     <li>검증 성공 시 {@link UserPrincipal} 을 만들어 {@link SecurityContext} 에 세팅.</li>
 *     <li>검증 실패 시 {@link BusinessException} 으로 throw — {@code GlobalExceptionHandler} 가 401 매핑.</li>
 * </ol>
 *
 * <h3>permitAll 경로</h3>
 * <p>경로 매칭은 {@code SecurityConfig.authorizeHttpRequests} 가 책임진다. 본 필터는 단순히 "토큰이 있으면 검증" 만 수행하며,
 * 토큰이 없는 요청은 그대로 통과시킨다. 인증이 필요한 경로에서 토큰이 없으면 Security 가 401 을 반환한다.</p>
 *
 * <h3>가상 스레드와의 상호작용</h3>
 * <p>본 필터의 모든 작업은 가상 스레드 위에서 자연스럽게 동작한다. JJWT 파싱은 마이크로초 단위 CPU 작업이라 carrier 점유 부담이 없다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = extractBearer(request);
        if (token != null) {
            try {
                Claims claims = jwtTokenProvider.parseAndValidate(token);
                Long userSeq = jwtTokenProvider.extractUserSeq(claims);
                String userId = jwtTokenProvider.extractUserId(claims);
                OauthService oauthService = jwtTokenProvider.extractOauthService(claims);

                UserPrincipal principal = new UserPrincipal(userSeq, userId, oauthService);
                AbstractAuthenticationToken auth = new JwtAuthentication(principal);
                auth.setDetails(token);
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (BusinessException ex) {
                // 검증 실패 — Security Context 비워 두고 컨트롤러까지 흘려보낸다.
                // 보호 경로면 Spring Security 가 AccessDenied/AuthenticationException 으로 401/403 처리.
                // 공개 경로면 그대로 통과.
                SecurityContextHolder.clearContext();
                if (log.isDebugEnabled()) {
                    log.debug("JWT 검증 실패: {} ({}), path={}", ex.getErrorCode(), ex.getMessage(), request.getRequestURI());
                }
            }
        }
        chain.doFilter(request, response);
    }

    private String extractBearer(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return header.substring(BEARER_PREFIX.length()).trim();
    }

    /**
     * JWT 기반 단순 인증 토큰. principal 은 {@link UserPrincipal}, credentials 는 null (필터 통과 후 더는 쓰지 않음).
     */
    static class JwtAuthentication extends AbstractAuthenticationToken {
        private final UserPrincipal principal;

        JwtAuthentication(UserPrincipal principal) {
            super(Collections.emptyList());
            this.principal = principal;
            setAuthenticated(true);
        }

        @Override
        public Object getCredentials() {
            return null;
        }

        @Override
        public Object getPrincipal() {
            return principal;
        }
    }
}
