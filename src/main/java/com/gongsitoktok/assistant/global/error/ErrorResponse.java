/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/global/error/ErrorResponse.java
 */
package com.gongsitoktok.assistant.global.error;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 공통 에러 응답 (제작요청 v6 §스텝 D).
 *
 * <pre>
 * {
 *   "traceId":   "8f2a...",
 *   "timestamp": "2026-06-17T10:23:11.123+09:00",
 *   "path":      "/api/v1/chat/room/12/continue",
 *   "status":    410,
 *   "code":      "CHAT_ROOM_EXPIRED",
 *   "message":   "30분 이상 활동이 없어 만료된 대화방입니다.",
 *   "fieldErrors": null
 * }
 * </pre>
 *
 * <p>{@code fieldErrors} 는 {@code @Valid} 실패 시에만 채워지며, 그 외는 {@code null}.
 * Jackson 의 {@link JsonInclude.Include#NON_NULL} 로 직렬화 시 자동 생략.</p>
 *
 * @param traceId     MDC traceId (응답 헤더 {@code X-Trace-Id} 와 동일)
 * @param timestamp   서버 발생 시각 (KST OffsetDateTime)
 * @param path        요청 경로
 * @param status      HTTP 상태 코드
 * @param code        클라이언트 분기용 enum 이름 ({@link ErrorCode#name()})
 * @param message     사용자에게 노출해도 안전한 한글 메시지
 * @param fieldErrors {@code @Valid} 실패 시의 필드 단위 오류 목록
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String traceId,
        OffsetDateTime timestamp,
        String path,
        int status,
        String code,
        String message,
        List<FieldError> fieldErrors
) {

    /**
     * 단일 필드 검증 실패 정보.
     *
     * @param field  필드명
     * @param reason 사용자에게 노출해도 안전한 사유
     */
    public record FieldError(String field, String reason) {
    }
}
