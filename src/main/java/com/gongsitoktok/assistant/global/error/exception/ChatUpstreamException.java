/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/global/error/exception/ChatUpstreamException.java
 */
package com.gongsitoktok.assistant.global.error.exception;

import com.gongsitoktok.assistant.global.error.ErrorCode;
import lombok.Getter;

/**
 * FastAPI 가 HTTP 200 으로 응답했지만 body 의 {@code error} 필드가 채워진 경우 (=AI 측 추론 실패).
 *
 * <p>{@code UpstreamErrorMapper} 가 FastAPI 의 원본 에러 코드를 클라이언트 노출용 {@link ErrorCode} 로 매핑한 뒤 본 예외로 throw 한다.
 * 정상 응답과 명확히 구분하기 위해 별도 클래스로 분리.</p>
 *
 * <h3>중요</h3>
 * <p>본 예외 발생 시 영속화({@code ChatPersistenceService}) 는 트리거하지 않는다 (§스텝 D · §스텝 E).</p>
 */
@Getter
public class ChatUpstreamException extends BusinessException {

    /** FastAPI 가 보고한 재시도 가능 여부 ({@code error.retriable}). */
    private final boolean retriable;

    public ChatUpstreamException(ErrorCode errorCode, String upstreamMessage, boolean retriable) {
        super(errorCode, upstreamMessage);
        this.retriable = retriable;
    }
}
