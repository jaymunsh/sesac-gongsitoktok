/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/chat/dto/ContinueRequest.java
 */
package com.gongsitoktok.assistant.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 기존 대화방 이어하기 요청 (제작요청 v6 §4-10).
 *
 * <p>클라이언트가 보낸 {@code corpCode} 는 절대 사용하지 않는다 — companyContext 는 항상 DB 의
 * {@code tb_chat_room.company} 값으로 고정 (다른 기업 우회 차단).</p>
 *
 * @param question 후속 질문
 */
@Schema(description = "기존 대화방 이어하기 요청")
public record ContinueRequest(
        @Schema(description = "질문 본문", example = "그럼 영업이익률은?")
        @NotBlank(message = "question 은 필수입니다.")
        @Size(max = 4000, message = "question 은 4000자 이하여야 합니다.")
        String question
) {
}
