/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/chat/dto/fastapi/FastApiVerification.java
 */
package com.gongsitoktok.assistant.chat.dto.fastapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * FastAPI 답변 신뢰도 검증 결과 — 프론트 신뢰도 뱃지 렌더.
 *
 * @param verdict        {@code "pass"} / {@code "partial"} / {@code "fail"} 중 하나
 * @param groundedScore  근거 결합도 (0.0 ~ 1.0)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FastApiVerification(String verdict, Double groundedScore) {
}
