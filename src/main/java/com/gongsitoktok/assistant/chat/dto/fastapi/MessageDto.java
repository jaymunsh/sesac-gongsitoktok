/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/chat/dto/fastapi/MessageDto.java
 */
package com.gongsitoktok.assistant.chat.dto.fastapi;

/**
 * FastAPI 요청의 {@code messages[]} 항목 — ChatGPT 호환 형식 {@code {role, content}}.
 *
 * <p>{@code role} 은 {@code "user"} 또는 {@code "assistant"} 두 종만 사용 (system 메시지는 FastAPI 내부 책임).</p>
 *
 * @param role    "user" 또는 "assistant"
 * @param content 발화 내용
 */
public record MessageDto(String role, String content) {

    public static MessageDto user(String content) {
        return new MessageDto("user", content);
    }

    public static MessageDto assistant(String content) {
        return new MessageDto("assistant", content);
    }
}
