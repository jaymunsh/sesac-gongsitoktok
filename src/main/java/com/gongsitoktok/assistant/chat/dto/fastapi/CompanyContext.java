/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/chat/dto/fastapi/CompanyContext.java
 */
package com.gongsitoktok.assistant.chat.dto.fastapi;

/**
 * FastAPI 요청의 {@code companyContext} 객체 — 한 방에 박힌 기업 식별·표시 정보를 동봉.
 *
 * <p>{@code /continue} 흐름에서는 클라이언트가 보낸 {@code corpCode} 를 절대 사용하지 않고, 서버가
 * {@code tb_chat_room.company} 에서 직접 읽어 채운다 (다른 기업 우회 차단, §4-10).</p>
 *
 * @param corpCode DART 기업 고유번호 (외부 노출 키)
 * @param corpName 기업명
 */
public record CompanyContext(String corpCode, String corpName) {
}
