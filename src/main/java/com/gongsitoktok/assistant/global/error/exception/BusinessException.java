/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/global/error/exception/BusinessException.java
 */
package com.gongsitoktok.assistant.global.error.exception;

import com.gongsitoktok.assistant.global.error.ErrorCode;
import lombok.Getter;

/**
 * 비즈니스 규칙 위반을 표현하는 공통 예외 베이스.
 *
 * <p>모든 도메인 예외(회사 미존재·방 만료·토큰 재사용 등) 는 본 클래스를 상속하여
 * {@link com.gongsitoktok.assistant.global.error.GlobalExceptionHandler} 가 일관된 형태로 응답을 만든다.</p>
 *
 * <p>가상 스레드 환경에서 예외 처리에 따른 carrier thread 점유 부담은 없다.</p>
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    /**
     * 기본 메시지({@link ErrorCode#getDefaultMessage()}) 를 사용.
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    /**
     * 임의의 메시지로 오버라이드 (디버깅·세부 사유 노출 시).
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
