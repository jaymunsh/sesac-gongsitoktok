/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/chat/dto/ChatResponse.java
 */
package com.gongsitoktok.assistant.chat.dto;

import com.gongsitoktok.assistant.chat.dto.fastapi.FastApiChatResponse;
import com.gongsitoktok.assistant.chat.dto.fastapi.FastApiSource;
import com.gongsitoktok.assistant.chat.dto.fastapi.FastApiVerification;
import com.gongsitoktok.assistant.chat.entity.ChatRoom;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 클라이언트에게 내려가는 챗봇 응답 (제작요청 v6 §4-9, §4-10).
 *
 * <p>FastAPI 응답({@link FastApiChatResponse}) 에서 {@code error} 필드를 제외한 모든 신호를 그대로 전달.
 * 프론트는 {@code outOfScope}/{@code needsClarification}/{@code verification} 등을 기반으로 UI 분기.</p>
 *
 * <p>{@code roomId} 는 서버가 결정 — {@code /ask} 응답에서는 신규 발급된 방의 id, {@code /continue} 응답에서는 path 의 roomId.</p>
 *
 * <h3>{@code lastActiveAt} 동봉 이유</h3>
 * <p>프론트 세션 타이머가 정확한 잔여 시간을 계산하려면 서버측 마지막 활동 시각이 필요. 매 응답마다 함께 내려
 * 클라이언트 시계 드리프트·새로고침 직후 hydrate 시 정확도를 확보한다. 백엔드는 {@code touch()} 직후의 값.</p>
 */
@Schema(description = "챗봇 응답 — FastAPI 부가 신호 + 세션 메타")
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
        FastApiVerification verification,
        LocalDateTime lastActiveAt
) {

    /**
     * FastAPI 응답 + 방 메타를 클라이언트 응답으로 변환.
     *
     * @param room 서버측 ChatRoom — roomId/lastActiveAt 추출용 (호출 측이 touch() 끝낸 상태)
     * @param r    FastAPI 응답
     */
    public static ChatResponse from(ChatRoom room, FastApiChatResponse r) {
        return new ChatResponse(
                room.getRoomId(),
                r.intent(),
                r.answerText(),
                r.sourceContent(),
                r.macroSnapshot(),
                r.sources(),
                r.outOfScope(),
                r.detectedCompany(),
                r.needsClarification(),
                r.verification(),
                room.getLastActiveAt()
        );
    }
}
