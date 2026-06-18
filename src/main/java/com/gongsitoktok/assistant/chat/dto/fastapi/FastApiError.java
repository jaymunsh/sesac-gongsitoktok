/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/chat/dto/fastapi/FastApiError.java
 */
package com.gongsitoktok.assistant.chat.dto.fastapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * FastAPI 가 HTTP 200 으로 응답했지만 추론 실패를 신호하는 경우의 {@code error} 객체.
 *
 * <p>HTTP 200 + {@code error != null} 패턴의 핵심 — "AI 가 처리는 시도했지만 실패" 와 "시스템이 못 받음" 을 명확히 구분.
 * 본 객체가 채워지면 Spring 은 {@code UpstreamErrorMapper} 로 코드를 변환해 throw 하고, 영속화는 트리거하지 않는다.</p>
 *
 * @param code      FastAPI 측 원본 코드 (예: {@code LLM_TIMEOUT})
 * @param message   사용자에게 노출 가능한 한글 메시지
 * @param retriable 재시도 가능 여부 — 프론트 UX 분기에 사용
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FastApiError(String code, String message, Boolean retriable) {
}
