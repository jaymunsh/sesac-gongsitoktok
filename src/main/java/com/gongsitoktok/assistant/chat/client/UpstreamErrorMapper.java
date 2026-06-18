/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/chat/client/UpstreamErrorMapper.java
 */
package com.gongsitoktok.assistant.chat.client;

import com.gongsitoktok.assistant.chat.dto.fastapi.FastApiError;
import com.gongsitoktok.assistant.global.error.ErrorCode;
import com.gongsitoktok.assistant.global.error.exception.ChatUpstreamException;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * FastAPI 측 원본 에러 코드를 클라이언트 노출용 {@link ErrorCode} 로 매핑 (연동명세서 산출물 v2.0 §5.2).
 *
 * <h3>매핑 원칙</h3>
 * <ul>
 *     <li>FastAPI 측 의미를 보존하면서 클라이언트가 분기 가능한 안정된 코드로 정규화.</li>
 *     <li>미정의 코드 → {@link ErrorCode#UPSTREAM_ERROR} 안전 폴백.</li>
 *     <li>FastAPI {@code INVALID_REQUEST} → {@code UPSTREAM_ERROR} 로 격하 — Spring 측 버그/계약 불일치를 내부 사고로 처리,
 *         사용자에겐 일반 메시지만 노출.</li>
 * </ul>
 */
@Component
public class UpstreamErrorMapper {

    private static final Map<String, ErrorCode> MAPPING = Map.ofEntries(
            Map.entry("LLM_TIMEOUT", ErrorCode.UPSTREAM_TIMEOUT),
            Map.entry("RATE_LIMITED", ErrorCode.UPSTREAM_RATE_LIMITED),
            Map.entry("CONTEXT_OVERFLOW", ErrorCode.CONTEXT_TOO_LONG),
            Map.entry("CONTENT_FILTERED", ErrorCode.CONTENT_FILTERED),
            Map.entry("VERIFICATION_FAILED_RETRY_EXHAUSTED", ErrorCode.ANSWER_NOT_TRUSTWORTHY),
            Map.entry("EMBEDDING_FAILED", ErrorCode.UPSTREAM_ERROR),
            Map.entry("VECTOR_SEARCH_FAILED", ErrorCode.UPSTREAM_ERROR),
            Map.entry("LLM_API_ERROR", ErrorCode.UPSTREAM_ERROR),
            Map.entry("INTERNAL_ERROR", ErrorCode.UPSTREAM_ERROR),
            Map.entry("INVALID_REQUEST", ErrorCode.UPSTREAM_ERROR)
    );

    /**
     * FastAPI {@code error} 객체 → 예외 변환.
     *
     * <p>{@code error.code} 가 null 이거나 매핑 없는 신규 코드면 {@link ErrorCode#UPSTREAM_ERROR} 폴백.
     * {@code error.message} 가 있으면 그대로 노출, 없으면 {@code ErrorCode.defaultMessage} 사용.</p>
     */
    public ChatUpstreamException toException(FastApiError error) {
        if (error == null) {
            return new ChatUpstreamException(ErrorCode.UPSTREAM_ERROR, "원인 미상의 추론 실패입니다.", false);
        }
        ErrorCode mapped = MAPPING.getOrDefault(error.code(), ErrorCode.UPSTREAM_ERROR);
        String msg = (error.message() != null && !error.message().isBlank())
                ? error.message()
                : mapped.getDefaultMessage();
        boolean retriable = Boolean.TRUE.equals(error.retriable());
        return new ChatUpstreamException(mapped, msg, retriable);
    }
}
