/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/chat/dto/fastapi/FastApiChatRequest.java
 */
package com.gongsitoktok.assistant.chat.dto.fastapi;

import java.util.List;

/**
 * FastAPI 단일 추론 endpoint 요청 바디 (연동명세서 산출물 v2.0 §3.2).
 *
 * <p>4 필드 외 다른 옵션은 허용하지 않음 — FastAPI 가 Pydantic strict 모드로 받기 때문에
 * 임의 필드 추가 시 400 응답.</p>
 *
 * @param roomId         {@code tb_chat_room.room_id}
 * @param userSeq        {@code tb_user.user_seq} (불변 PK; PII 최소화 + dismiss 안전)
 * @param companyContext 현재 방에 박힌 기업 정보
 * @param messages       Multi-turn 이력 + 새 사용자 발화 (마지막 항목)
 */
public record FastApiChatRequest(
        Long roomId,
        Long userSeq,
        CompanyContext companyContext,
        List<MessageDto> messages
) {
}
