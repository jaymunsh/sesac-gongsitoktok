/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/chat/dto/ChatMessagesResponse.java
 */
package com.gongsitoktok.assistant.chat.dto;

import com.gongsitoktok.assistant.chat.entity.QaHistory;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 대화방 타임라인 응답 (제작요청 v6 §4-12).
 *
 * <h3>활성/만료 메타 동봉</h3>
 * <p>프론트가 URL queryParam 의 roomId 로 페이지에 진입할 때 한 호출로 활성/만료 + 30분 잔여 시간까지 판단하도록
 * {@code isActive} / {@code lastActiveAt} / {@code closedAt} 을 함께 내려준다.</p>
 * <ul>
 *     <li>{@code isActive=true} + 미만료 → ChatPanel 정상 모드 (입력 가능, 타이머 동작)</li>
 *     <li>{@code isActive=false} → ChatPanel 만료 모드 (입력 disable, "새 채팅 시작" 배너 표시)</li>
 * </ul>
 *
 * @param roomId        방 id
 * @param corpCode      방에 박힌 기업 corpCode
 * @param corpName      방에 박힌 기업명
 * @param isActive      세션 활성 여부 (false 면 만료 또는 명시적 close 됨)
 * @param lastActiveAt  마지막 활동 시각 — 프론트 타이머가 (lastActiveAt + 30분 - now) 로 잔여 시간 계산
 * @param closedAt      세션 종료 시각 (isActive=true 면 null)
 * @param messages      과거 Q&A 시간순 (createdAt ASC)
 */
@Schema(description = "대화방 타임라인")
public record ChatMessagesResponse(
        Long roomId,
        String corpCode,
        String corpName,
        boolean isActive,
        LocalDateTime lastActiveAt,
        LocalDateTime closedAt,
        List<Item> messages
) {

    /**
     * 타임라인 단일 turn.
     *
     * @param groundedScore  답변 신뢰도 0.0~1.0 (nullable). 프론트의 출처 모달 우측 하단 "정확도" 뱃지에 사용
     */
    @Schema(description = "Q&A 한 턴")
    public record Item(
            String question,
            String answer,
            String sourceContent,
            String macroSnapshot,
            Double groundedScore,
            LocalDateTime createdAt
    ) {
        public static Item from(QaHistory q) {
            return new Item(
                    q.getQuestion(),
                    q.getAnswer(),
                    q.getSourceContent(),
                    q.getMacroSnapshot(),
                    q.getGroundedScore(),
                    q.getCreatedAt()
            );
        }
    }
}
