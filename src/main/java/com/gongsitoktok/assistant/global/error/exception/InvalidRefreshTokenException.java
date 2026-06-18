/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/global/error/exception/InvalidRefreshTokenException.java
 */
package com.gongsitoktok.assistant.global.error.exception;

import com.gongsitoktok.assistant.global.error.ErrorCode;

/**
 * Refresh Token 이 DB 에 없거나, revoke 되었거나, 만료되었을 때 발생.
 *
 * <p>재사용 탐지({@link RefreshTokenReusedException}) 와는 명확히 구분된다 — 재사용은 추가로 전체 세션을 종료시킨다.</p>
 */
public class InvalidRefreshTokenException extends BusinessException {

    public InvalidRefreshTokenException() {
        super(ErrorCode.INVALID_REFRESH_TOKEN);
    }

    public InvalidRefreshTokenException(ErrorCode errorCode) {
        super(errorCode);
    }
}
