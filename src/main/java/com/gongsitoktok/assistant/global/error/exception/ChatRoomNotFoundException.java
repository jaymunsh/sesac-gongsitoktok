/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/global/error/exception/ChatRoomNotFoundException.java
 */
package com.gongsitoktok.assistant.global.error.exception;

import com.gongsitoktok.assistant.global.error.ErrorCode;

/**
 * 대화방 미존재 · 비활성(hide) · 타인 소유 시 모두 동일하게 발생.
 *
 * <p>"숨겨졌다" 거나 "당신 것이 아니다" 같은 사실을 노출하지 않고 단일 404 로 응답하여 정보 누출 차단.</p>
 */
public class ChatRoomNotFoundException extends BusinessException {

    public ChatRoomNotFoundException(Long roomId) {
        super(ErrorCode.CHAT_ROOM_NOT_FOUND, "대화방을 찾을 수 없습니다: " + roomId);
    }
}
