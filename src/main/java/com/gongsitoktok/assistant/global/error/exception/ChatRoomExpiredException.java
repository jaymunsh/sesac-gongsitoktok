/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/global/error/exception/ChatRoomExpiredException.java
 */
package com.gongsitoktok.assistant.global.error.exception;

import com.gongsitoktok.assistant.global.error.ErrorCode;

/**
 * {@code lastActiveAt + 30분 < now} 인 대화방에서 {@code /continue} 가 호출될 때 발생.
 *
 * <p>HTTP 410 (Gone) + {@code CHAT_ROOM_EXPIRED}. 프론트는 본 코드 수신 시 {@code /ask} 흐름으로 새 방을 생성한다.</p>
 */
public class ChatRoomExpiredException extends BusinessException {

    public ChatRoomExpiredException(Long roomId) {
        super(ErrorCode.CHAT_ROOM_EXPIRED, "만료된 대화방입니다: " + roomId);
    }
}
