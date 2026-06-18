/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/chat/dto/fastapi/FastApiChatResponse.java
 */
package com.gongsitoktok.assistant.chat.dto.fastapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * FastAPI 단일 추론 endpoint 응답 바디 (연동명세서 산출물 v2.0 §4).
 *
 * <h3>필드 분류</h3>
 * <ul>
 *     <li><b>기본</b>      — {@code roomId, intent, answerText, sourceContent, macroSnapshot}</li>
 *     <li><b>출처</b>      — {@code sources[]} (구조화 출처 카드)</li>
 *     <li><b>부가 신호</b> — {@code outOfScope, detectedCompany, needsClarification, verification}</li>
 *     <li><b>에러</b>      — {@code error} (HTTP 200 + 채움 시 추론 실패)</li>
 * </ul>
 *
 * <p>{@code intent} 는 {@code "qa" | "summary" | "macro" | "smalltalk" | "out_of_scope"} 중 하나. enum 으로 좁히지 않고
 * 문자열로 받는 이유는 FastAPI 측 신규 의도 추가를 깨지 않기 위함.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FastApiChatResponse(
        Long roomId,
        String intent,
        String answerText,
        String sourceContent,
        String macroSnapshot,
        List<FastApiSource> sources,
        Boolean outOfScope,
        String detectedCompany,
        Boolean needsClarification,
        FastApiVerification verification,
        FastApiError error
) {
}
