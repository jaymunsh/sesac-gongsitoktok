/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/auth/dto/TokenResponse.java
 */
package com.gongsitoktok.assistant.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 로그인 / Refresh 회전 응답 바디 (제작요청 v6 §4-2).
 *
 * <p>Refresh Token 자체는 본 바디가 아니라 {@code Set-Cookie: refreshToken=...} httpOnly 쿠키로 별도 전송한다.</p>
 *
 * @param accessToken Access Token (JWT)
 * @param tokenType   항상 {@code "Bearer"}
 * @param expiresIn   Access Token 만료까지 남은 초 (기본 3600)
 */
@Schema(description = "로그인/리프레시 응답 — Refresh Token 은 별도 httpOnly 쿠키로 전송")
public record TokenResponse(
        @Schema(description = "Access Token (JWT)", example = "eyJhbGciOi...")
        String accessToken,

        @Schema(description = "토큰 타입", example = "Bearer")
        String tokenType,

        @Schema(description = "Access Token 만료까지 남은 초", example = "3600")
        long expiresIn
) {
    public static TokenResponse bearer(String accessToken, long expiresInSeconds) {
        return new TokenResponse(accessToken, "Bearer", expiresInSeconds);
    }
}
