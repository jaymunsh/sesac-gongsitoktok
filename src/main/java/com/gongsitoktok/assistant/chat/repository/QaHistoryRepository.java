/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/chat/repository/QaHistoryRepository.java
 */
package com.gongsitoktok.assistant.chat.repository;

import com.gongsitoktok.assistant.chat.entity.QaHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * QA 이력 리포지토리.
 *
 * <p>핵심 쿼리 패턴:</p>
 * <ul>
 *     <li>{@code findByChatRoomRoomIdOrderByCreatedAtAsc} — 멀티턴 messages 빌드(§4-10) + 타임라인(§4-12) 공용.</li>
 * </ul>
 */
public interface QaHistoryRepository extends JpaRepository<QaHistory, Long> {

    /**
     * 특정 대화방의 QA 를 생성 시각 오름차순으로 조회.
     */
    List<QaHistory> findByChatRoomRoomIdOrderByCreatedAtAsc(Long roomId);
}
