/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/global/security/RestAccessDeniedHandler.java
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * 인증은 됐지만 권한이 부족할 때 403 JSON 응답 (제작요청 v6 §스텝 D).
 *
 * <p>Spring Security 기본은 403 + 빈 본문. REST 표준에 맞춰 {@link ErrorResponse} 포맷 JSON 으로 통일.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        log.warn("403 forbidden — path={}, reason={}", request.getRequestURI(), accessDeniedException.getMessage());
        ErrorResponse body = new ErrorResponse(
                MDC.get(MdcTraceIdFilter.MDC_TRACE_ID),
                OffsetDateTime.now(KST),
                request.getRequestURI(),
                HttpStatus.FORBIDDEN.value(),
                ErrorCode.INVALID_TOKEN.name(),
                "접근 권한이 없습니다.",
                null
        );
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
