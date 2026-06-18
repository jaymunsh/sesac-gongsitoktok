/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/chat/dto/ChatRoomListItemResponse.java
 */
package com.gongsitoktok.assistant.chat.dto;

import com.gongsitoktok.assistant.chat.entity.ChatRoom;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 사이드바 대화방 목록 항목 (제작요청 v6 §4-11).
 *
 * <p>{@code corpCode}/{@code corpName} 은 JOIN FETCH 로 한 쿼리에 묶어 조회.</p>
 */
@Schema(description = "대화방 목록 항목")
public record ChatRoomListItemResponse(
        Long roomId,
        String roomTitle,
        String corpCode,
        String corpName,
        LocalDateTime lastActiveAt
) {
    public static ChatRoomListItemResponse from(ChatRoom r) {
        return new ChatRoomListItemResponse(
                r.getRoomId(),
                r.getRoomTitle(),
                r.getCompany().getCorpCode(),
                r.getCompany().getCorpName(),
                r.getLastActiveAt()
        );
    }
}
