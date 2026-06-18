/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/global/error/exception/UpstreamUnavailableException.java
 */
package com.gongsitoktok.assistant.global.error.exception;

import com.gongsitoktok.assistant.global.error.ErrorCode;

/**
 * FastAPI 가 4xx/5xx HTTP 상태를 반환하거나 Connect Timeout 이 발생한 경우.
 *
 * <p>body 파싱은 시도하지 않으며, HTTP status 만으로 분기한다 (Spring_챗봇_핸들링_가이드 §4).</p>
 */
public class UpstreamUnavailableException extends BusinessException {

    public UpstreamUnavailableException() {
        super(ErrorCode.UPSTREAM_UNAVAILABLE);
    }

    public UpstreamUnavailableException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public UpstreamUnavailableException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
