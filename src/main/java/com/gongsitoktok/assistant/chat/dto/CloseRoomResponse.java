/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/chat/dto/CloseRoomResponse.java
 */
package com.gongsitoktok.assistant.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 대화방 명시적 종료(close) 응답.
 *
 * <h3>의미</h3>
 * <p>{@code POST /api/v1/chat/room/{roomId}/close} 호출 시 반환. 30분 자동 만료를 기다리지 않고
 * 즉시 세션을 닫는 경로 — 테스트/QA 용도이자, 추후 "대화 종료" 버튼이 추가될 경우의 백엔드 거점.</p>
 *
 * <h3>멱등성</h3>
 * <p>이미 닫혀 있는 방을 다시 close 해도 동일한 응답을 반환. {@code alreadyClosed=true} 로 호출 측에서 구분 가능.
 * 이 경우 {@code closedAt} 은 <b>최초 close 시점</b> 그대로 (재호출로 갱신되지 않음 — {@link com.gongsitoktok.assistant.chat.entity.ChatRoom#close} 의 멱등 보장).</p>
 *
 * @param roomId        닫힌 대화방 식별자
 * @param closedAt      세션이 실제로 종료된 시각 (멱등 호출 시 최초 종료 시각 유지)
 * @param alreadyClosed 호출 전에 이미 닫혀 있었는지 여부
 */
@Schema(description = "대화방 명시적 종료 응답")
public record CloseRoomResponse(
        @Schema(description = "닫힌 대화방 식별자", example = "42")
        Long roomId,

        @Schema(description = "세션 종료 시각 (멱등 호출 시 최초 종료 시각 유지)", example = "2026-06-20T13:24:11")
        LocalDateTime closedAt,

        @Schema(description = "호출 전에 이미 닫혀 있던 방이었는지 여부", example = "false")
        boolean alreadyClosed
) {
    public static CloseRoomResponse of(Long roomId, LocalDateTime closedAt, boolean alreadyClosed) {
        return new CloseRoomResponse(roomId, closedAt, alreadyClosed);
    }
}
