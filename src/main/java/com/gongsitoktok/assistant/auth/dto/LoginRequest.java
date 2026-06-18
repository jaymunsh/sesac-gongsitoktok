/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/auth/dto/LoginRequest.java
 */
package com.gongsitoktok.assistant.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 로컬 로그인 요청 바디. OAuth 로그인은 본 DTO 와 무관하며 {@code /oauth2/authorization/{provider}} 로 시작.
 *
 * @param userId   로그인 ID
 * @param password 평문 비밀번호 (서버에서 BCrypt 비교)
 */
@Schema(description = "로컬 로그인 요청")
public record LoginRequest(
        @Schema(description = "로그인 ID", example = "alice01")
        @NotBlank(message = "userId 는 필수입니다.")
        String userId,

        @Schema(description = "평문 비밀번호", example = "Passw0rd!")
        @NotBlank(message = "password 는 필수입니다.")
        String password
) {
}
