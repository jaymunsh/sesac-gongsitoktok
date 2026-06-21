/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/chat/dto/ActiveChatRoomResponse.java
 */
package com.gongsitoktok.assistant.chat.dto;

import com.gongsitoktok.assistant.chat.entity.ChatRoom;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 활성 채팅방 lookup 응답 — {@code GET /api/v1/chat/room/active?corpCode=...}.
 *
 * <h3>호출 시점</h3>
 * <p>프론트 CompanyPage 마운트 시점 + 윈도우 포커스 회복 시점에 호출되어
 * "이 사용자의 이 기업 활성 방" 단건을 조회. 있으면 {@code roomId} 로 {@code /messages} 를 이어 호출해
 * 히스토리 복원, 없으면 빈 패널.</p>
 *
 * <h3>활성 방이 없는 경우</h3>
 * <p>Controller 가 {@code 204 No Content} 로 응답. 404 는 부적절 — 기업 자체는 존재하는데
 * "활성 방 없음" 은 정상 상태.</p>
 *
 * <h3>SOFT 단일 활성 방 정책</h3>
 * <p>DB unique constraint 가 없어 race 케이스에서 활성 방이 잠시 2개 이상 공존할 수 있다.
 * Service 가 {@code lastActiveAt DESC LIMIT 1} 채택 + 나머지 lazy close 처리하므로
 * 응답에는 항상 단일 방만 노출된다.</p>
 *
 * @param roomId        활성 방 식별자
 * @param corpCode      방에 박힌 기업 corpCode (요청한 값과 동일하나 명시 노출로 클라이언트 검증 용이)
 * @param corpName      기업명
 * @param lastActiveAt  마지막 활동 시각 — 프론트 세션 타이머 잔여 계산 기준
 */
@Schema(description = "활성 채팅방 lookup 응답")
public record ActiveChatRoomResponse(
        @Schema(description = "활성 방 식별자", example = "42")
        Long roomId,

        @Schema(description = "기업 corpCode", example = "00126380")
        String corpCode,

        @Schema(description = "기업명", example = "삼성전자")
        String corpName,

        @Schema(description = "마지막 활동 시각 (세션 타이머 기준점)", example = "2026-06-20T13:24:11")
        LocalDateTime lastActiveAt
) {
    public static ActiveChatRoomResponse from(ChatRoom room) {
        return new ActiveChatRoomResponse(
                room.getRoomId(),
                room.getCompany().getCorpCode(),
                room.getCompany().getCorpName(),
                room.getLastActiveAt()
        );
    }
}
