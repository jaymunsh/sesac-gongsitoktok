/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/chat/dto/AskRequest.java
 */
package com.gongsitoktok.assistant.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 최초 질문 요청 (제작요청 v6 §4-9 {@code /chat/ask}).
 *
 * @param corpCode 현재 보고 있는 기업의 DART 고유번호
 * @param question 사용자 질문 (TEXT, 4000자 마진)
 */
@Schema(description = "최초 질문 요청 — 대화방 자동 생성")
public record AskRequest(
        @Schema(description = "기업 DART 고유번호", example = "00126380")
        @NotBlank(message = "corpCode 는 필수입니다.")
        @Size(max = 32, message = "corpCode 형식이 잘못되었습니다.")
        String corpCode,

        @Schema(description = "질문 본문", example = "이 회사 최근 매출 동향 알려줘")
        @NotBlank(message = "question 은 필수입니다.")
        @Size(max = 4000, message = "question 은 4000자 이하여야 합니다.")
        String question
) {
}
