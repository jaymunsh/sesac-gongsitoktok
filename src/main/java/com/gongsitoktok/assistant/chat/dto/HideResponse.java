/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/chat/dto/HideResponse.java
 */
package com.gongsitoktok.assistant.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 대화방 숨김 응답 (제작요청 v6 §4-13).
 *
 * <p>이미 숨겨진 방은 멱등 처리 — {@code hiddenAt} 은 기존 값 그대로 반환.</p>
 */
@Schema(description = "대화방 숨김 응답")
public record HideResponse(LocalDateTime hiddenAt) {
}
