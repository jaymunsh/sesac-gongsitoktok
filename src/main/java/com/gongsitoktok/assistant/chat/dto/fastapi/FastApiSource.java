/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/chat/dto/fastapi/FastApiSource.java
 */
package com.gongsitoktok.assistant.chat.dto.fastapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * FastAPI 응답의 {@code sources[]} 항목 — 프론트가 출처 카드로 렌더링.
 *
 * <p>{@link JsonIgnoreProperties#ignoreUnknown()} = {@code true} — FastAPI 측이 필드를 추가해도 Spring 이 깨지지 않도록.</p>
 *
 * @param rceptNo      DART 공시 접수번호
 * @param reportNm     보고서명
 * @param rceptDt      접수일자 (YYYYMMDD)
 * @param sectionTitle 인용 섹션 제목
 * @param quote        인용 본문 짧은 발췌
 * @param score        검색 관련도 점수 (0.0 ~ 1.0)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FastApiSource(
        String rceptNo,
        String reportNm,
        String rceptDt,
        String sectionTitle,
        String quote,
        Double score
) {
}
