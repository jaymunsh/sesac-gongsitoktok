/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/global/error/exception/UserWithdrawnException.java
 */
package com.gongsitoktok.assistant.global.error.exception;

import com.gongsitoktok.assistant.global.error.ErrorCode;

/**
 * {@code isActive=false} 사용자가 로그인을 시도하거나, 토큰 검증 시 비활성 사용자가 감지될 때 발생.
 */
public class UserWithdrawnException extends BusinessException {

    public UserWithdrawnException() {
        super(ErrorCode.USER_WITHDRAWN);
    }
}
