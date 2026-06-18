/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/chat/repository/ChatRoomRepository.java
 */
package com.gongsitoktok.assistant.chat.repository;

import com.gongsitoktok.assistant.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 대화방 리포지토리.
 *
 * <p>본인 소유 + isActive=true 필터링은 모두 서비스 레이어에서 명시적으로 적용한다 (§4-11·§4-12·§4-13).</p>
 */
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    /**
     * 특정 유저의 활성 대화방을 최근 활동 순으로 조회 (§4-11 사이드바).
     *
     * <p>{@code company} 를 JOIN FETCH 하여 N+1 차단.</p>
     */
    @Query("""
            SELECT r FROM ChatRoom r
              JOIN FETCH r.company c
             WHERE r.user.userSeq = :userSeq
               AND r.isActive = true
             ORDER BY r.lastActiveAt DESC
            """)
    List<ChatRoom> findActiveByUserSeqWithCompany(@Param("userSeq") Long userSeq);

    /**
     * 단건 조회 — {@code company} 함께 fetch.
     * <p>본인 소유 검증·만료 검증은 서비스 레이어 책임.</p>
     */
    @Query("""
            SELECT r FROM ChatRoom r
              JOIN FETCH r.company c
             WHERE r.roomId = :roomId
            """)
    Optional<ChatRoom> findByIdWithCompany(@Param("roomId") Long roomId);
}
