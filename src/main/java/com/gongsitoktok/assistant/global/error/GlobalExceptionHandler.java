/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/global/error/GlobalExceptionHandler.java
 */
package com.gongsitoktok.assistant.global.error;

import com.gongsitoktok.assistant.global.error.exception.BusinessException;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * 전역 예외 처리기 — 모든 컨트롤러 예외를 {@link ErrorResponse} 형태의 JSON 으로 일관 변환.
 *
 * <h3>핵심 동작</h3>
 * <ul>
 *     <li>{@link BusinessException} → 첨부된 {@link ErrorCode} 의 HTTP 상태 + 코드명으로 응답.</li>
 *     <li>{@code @Valid} 실패 → {@link ErrorCode#VALIDATION_FAILED} + {@code fieldErrors} 배열.</li>
 *     <li>Spring Security 예외 → 401 / 403 매핑.</li>
 *     <li>기타 미분류 예외 → {@link ErrorCode#INTERNAL_BUG} (500). 스택 트레이스는 서버 로그에만 남기고 메시지는 일반화.</li>
 * </ul>
 *
 * <h3>관측성</h3>
 * <ul>
 *     <li>응답 JSON 의 {@code traceId} 와 응답 헤더의 {@code X-Trace-Id} 가 동일하도록 MDC 값을 그대로 사용.</li>
 *     <li>비즈니스 예외는 WARN, 시스템 예외는 ERROR 로 분리 로깅.</li>
 * </ul>
 *
 * <p>Swagger 문서 표면에는 {@link Hidden} 으로 가려 운영자가 볼 endpoint 카운트에 잡히지 않게 한다.</p>
 */
@Hidden
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    /**
     * 비즈니스 규칙 위반 — {@link ErrorCode} 의 status 사용.
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex, HttpServletRequest req) {
        ErrorCode code = ex.getErrorCode();
        log.warn("[BIZ] code={}, msg={}, path={}", code.name(), ex.getMessage(), req.getRequestURI());
        return build(code.getStatus(), code, ex.getMessage(), req, null);
    }

    /**
     * {@code @Valid @RequestBody} 실패. 필드별 오류 목록을 함께 노출.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<ErrorResponse.FieldError> fields = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldError)
                .toList();
        log.warn("[VALID] fields={}, path={}", fields, req.getRequestURI());
        return build(ErrorCode.VALIDATION_FAILED.getStatus(), ErrorCode.VALIDATION_FAILED,
                ErrorCode.VALIDATION_FAILED.getDefaultMessage(), req, fields);
    }

    /**
     * {@code @Valid} 가 직접 던지는 또 다른 케이스 (폼 바인딩 등).
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBind(BindException ex, HttpServletRequest req) {
        List<ErrorResponse.FieldError> fields = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldError)
                .toList();
        return build(ErrorCode.VALIDATION_FAILED.getStatus(), ErrorCode.VALIDATION_FAILED,
                ErrorCode.VALIDATION_FAILED.getDefaultMessage(), req, fields);
    }

    /**
     * 요청 본문 자체가 JSON 파싱 불가.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return build(ErrorCode.VALIDATION_FAILED.getStatus(), ErrorCode.VALIDATION_FAILED,
                "요청 본문을 해석할 수 없습니다.", req, null);
    }

    /**
     * Path/Query 파라미터 타입 불일치.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        String msg = "파라미터 타입이 올바르지 않습니다: " + ex.getName();
        return build(ErrorCode.VALIDATION_FAILED.getStatus(), ErrorCode.VALIDATION_FAILED, msg, req, null);
    }

    /**
     * 정의되지 않은 메서드.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, ErrorCode.VALIDATION_FAILED,
                "허용되지 않은 HTTP 메서드입니다: " + ex.getMethod(), req, null);
    }

    /**
     * 정의되지 않은 경로 (Spring 의 throw-no-handler 활성화 시).
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandler(NoHandlerFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, ErrorCode.VALIDATION_FAILED,
                "요청 경로를 찾을 수 없습니다: " + ex.getRequestURL(), req, null);
    }

    /**
     * 인증 실패 — 토큰 자체가 유효하지 않거나 누락.
     */
    @ExceptionHandler({AuthenticationException.class, AuthenticationCredentialsNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleAuth(AuthenticationException ex, HttpServletRequest req) {
        log.warn("[AUTH] {}: {}", ex.getClass().getSimpleName(), ex.getMessage());
        return build(ErrorCode.INVALID_TOKEN.getStatus(), ErrorCode.INVALID_TOKEN,
                ErrorCode.INVALID_TOKEN.getDefaultMessage(), req, null);
    }

    /**
     * 인가 실패 — 인증은 통과했으나 권한 없음.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleDenied(AccessDeniedException ex, HttpServletRequest req) {
        log.warn("[FORBIDDEN] path={}, msg={}", req.getRequestURI(), ex.getMessage());
        return build(HttpStatus.FORBIDDEN, ErrorCode.INVALID_TOKEN,
                "접근 권한이 없습니다.", req, null);
    }

    /**
     * 미분류 시스템 예외 — 스택 트레이스는 서버 로그에만, 메시지는 일반화.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnknown(Exception ex, HttpServletRequest req) {
        log.error("[UNHANDLED] path={}", req.getRequestURI(), ex);
        return build(ErrorCode.INTERNAL_BUG.getStatus(), ErrorCode.INTERNAL_BUG,
                ErrorCode.INTERNAL_BUG.getDefaultMessage(), req, null);
    }

    // ===== 내부 헬퍼 =====

    private ResponseEntity<ErrorResponse> build(HttpStatus status, ErrorCode code, String message,
                                                HttpServletRequest req,
                                                List<ErrorResponse.FieldError> fieldErrors) {
        ErrorResponse body = new ErrorResponse(
                MDC.get("traceId"),
                OffsetDateTime.now(KST),
                req.getRequestURI(),
                status.value(),
                code.name(),
                message,
                fieldErrors == null || fieldErrors.isEmpty() ? null : fieldErrors
        );
        return ResponseEntity.status(status).body(body);
    }

    private ErrorResponse.FieldError toFieldError(FieldError f) {
        return new ErrorResponse.FieldError(f.getField(),
                f.getDefaultMessage() != null ? f.getDefaultMessage() : "유효하지 않은 값입니다.");
    }
}
