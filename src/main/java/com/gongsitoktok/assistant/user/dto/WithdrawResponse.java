/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/user/dto/WithdrawResponse.java
 */
package com.gongsitoktok.assistant.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 회원 탈퇴 응답 (제작요청 v6 §4-6).
 *
 * @param withdrawnAt       탈퇴 시각 (이미 탈퇴된 경우 기존 값)
 * @param alreadyWithdrawn  이미 탈퇴된 상태였는지 여부 (멱등 처리 신호)
 */
@Schema(description = "회원 탈퇴 응답 — 멱등 처리")
public record WithdrawResponse(
        @Schema(description = "탈퇴 시각") LocalDateTime withdrawnAt,
        @Schema(description = "이미 탈퇴된 상태였는지") boolean alreadyWithdrawn
) {
}
