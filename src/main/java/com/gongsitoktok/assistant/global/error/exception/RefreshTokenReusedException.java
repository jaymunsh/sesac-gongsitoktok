/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/global/error/exception/RefreshTokenReusedException.java
 */
package com.gongsitoktok.assistant.global.error.exception;

import com.gongsitoktok.assistant.global.error.ErrorCode;

/**
 * 이미 revoke 된 Refresh Token 이 재사용된 도난 의심 시나리오.
 *
 * <p>본 예외 발생 즉시 {@code RefreshTokenService.revokeAllByUser(userSeq)} 호출로 해당 유저의 모든 Refresh Token 을
 * 일괄 무효화 → 강제 전체 재로그인 유도.</p>
 */
public class RefreshTokenReusedException extends BusinessException {

    public RefreshTokenReusedException() {
        super(ErrorCode.REFRESH_TOKEN_REUSED);
    }
}
