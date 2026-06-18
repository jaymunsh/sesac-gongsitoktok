/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/user/dto/PasswordChangeRequest.java
 */
package com.gongsitoktok.assistant.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 비밀번호 변경 요청 (제작요청 v6 §4-5, LOCAL 사용자 전용).
 *
 * <p>새 비밀번호 정책({@code PASSWORD_POLICY_VIOLATION}) 은 서비스 레이어에서 명시 검증.</p>
 */
@Schema(description = "비밀번호 변경 요청 — LOCAL 사용자 전용")
public record PasswordChangeRequest(
        @Schema(description = "현재 비밀번호 (BCrypt 검증)")
        @NotBlank(message = "currentPassword 는 필수입니다.")
        String currentPassword,

        @Schema(description = "새 비밀번호 (정책 검증)")
        @NotBlank(message = "newPassword 는 필수입니다.")
        @Size(min = 8, max = 100, message = "newPassword 는 8~100자여야 합니다.")
        String newPassword
) {
}
