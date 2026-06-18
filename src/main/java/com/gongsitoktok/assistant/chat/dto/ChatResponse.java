/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/chat/dto/ChatResponse.java
 */
package com.gongsitoktok.assistant.chat.dto;

import com.gongsitoktok.assistant.chat.dto.fastapi.FastApiChatResponse;
import com.gongsitoktok.assistant.chat.dto.fastapi.FastApiSource;
import com.gongsitoktok.assistant.chat.dto.fastapi.FastApiVerification;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 클라이언트에게 내려가는 챗봇 응답 (제작요청 v6 §4-9, §4-10).
 *
 * <p>FastAPI 응답({@link FastApiChatResponse}) 에서 {@code error} 필드를 제외한 모든 신호를 그대로 전달.
 * 프론트는 {@code outOfScope}/{@code needsClarification}/{@code verification} 등을 기반으로 UI 분기.</p>
 *
 * <p>{@code roomId} 는 서버가 결정 — {@code /ask} 응답에서는 신규 발급된 방의 id, {@code /continue} 응답에서는 path 의 roomId.</p>
 */
@Schema(description = "챗봇 응답 — FastAPI 부가 신호 그대로 전달")
public record ChatResponse(
        Long roomId,
        String intent,
        String answerText,
        String sourceContent,
        String macroSnapshot,
        List<FastApiSource> sources,
        Boolean outOfScope,
        String detectedCompany,
        Boolean needsClarification,
        FastApiVerification verification
) {

    /**
     * FastAPI 응답을 클라이언트 응답으로 변환.
     *
     * @param roomId 서버가 결정한 방 id (FastAPI 응답이 같은 값을 echo 할 수도 있으나 서버 값을 우선)
     */
    public static ChatResponse from(Long roomId, FastApiChatResponse r) {
        return new ChatResponse(
                roomId,
                r.intent(),
                r.answerText(),
                r.sourceContent(),
                r.macroSnapshot(),
                r.sources(),
                r.outOfScope(),
                r.detectedCompany(),
                r.needsClarification(),
                r.verification()
        );
    }
}
