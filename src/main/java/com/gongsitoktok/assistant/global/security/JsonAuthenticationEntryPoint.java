/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/global/security/JsonAuthenticationEntryPoint.java
 */
package com.gongsitoktok.assistant.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gongsitoktok.assistant.global.error.ErrorCode;
import com.gongsitoktok.assistant.global.error.ErrorResponse;
import com.gongsitoktok.assistant.global.filter.MdcTraceIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * 보호 경로 무인증 접근 시 401 JSON 응답 (제작요청 v6 §스텝 D).
 *
 * <h3>왜 필요한가</h3>
 * <p>Spring Security 기본 {@link AuthenticationEntryPoint} 는 로그인 페이지로 302 redirect 한다. 본 프로젝트는
 * REST + JWT 구조이므로 무인증 접근 시 {@link com.gongsitoktok.assistant.global.error.ErrorResponse} 포맷의
 * 401 JSON 을 반환해야 한다 ({@code GlobalExceptionHandler} 가 만드는 응답과 일관).</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        if (log.isDebugEnabled()) {
            log.debug("401 entry — path={}, reason={}", request.getRequestURI(), authException.getMessage());
        }
        ErrorResponse body = new ErrorResponse(
                MDC.get(MdcTraceIdFilter.MDC_TRACE_ID),
                OffsetDateTime.now(KST),
                request.getRequestURI(),
                ErrorCode.INVALID_TOKEN.getStatus().value(),
                ErrorCode.INVALID_TOKEN.name(),
                ErrorCode.INVALID_TOKEN.getDefaultMessage(),
                null
        );
        response.setStatus(ErrorCode.INVALID_TOKEN.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
