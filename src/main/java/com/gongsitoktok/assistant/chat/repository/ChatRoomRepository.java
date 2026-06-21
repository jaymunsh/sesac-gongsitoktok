/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/chat/repository/ChatRoomRepository.java
 */
package com.gongsitoktok.assistant.chat.repository;

import com.gongsitoktok.assistant.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 대화방 리포지토리 (v2 기획 — {@code isActive} 의미 = 세션 활성 여부).
 *
 * <p>본인 소유 검증·만료 검증은 모두 서비스 레이어에서 명시적으로 적용한다.</p>
 */
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    /**
     * 마이페이지 노출 목록 — {@code isActive=false} (만료된 방) 만 최근 활동 순으로 조회.
     *
     * <p>{@code company} 를 JOIN FETCH 하여 N+1 차단.</p>
     */
    @Query("""
            SELECT r FROM ChatRoom r
              JOIN FETCH r.company c
             WHERE r.user.userSeq = :userSeq
               AND r.isActive = false
             ORDER BY r.lastActiveAt DESC
            """)
    List<ChatRoom> findClosedByUserSeqWithCompany(@Param("userSeq") Long userSeq);

    /**
     * 만료 임계({@code lastActiveAt + 30분}) 를 이미 넘긴 활성 방 일괄 조회 — 마이페이지 조회 시점에 lazy close 처리용.
     *
     * <p>{@code expiredBefore} 파라미터는 호출 측에서 {@code now.minusMinutes(30)} 를 전달.</p>
     */
    @Query("""
            SELECT r FROM ChatRoom r
             WHERE r.user.userSeq = :userSeq
               AND r.isActive = true
               AND r.lastActiveAt < :expiredBefore
            """)
    List<ChatRoom> findActiveExpiredByUserSeq(@Param("userSeq") Long userSeq,
                                              @Param("expiredBefore") LocalDateTime expiredBefore);

    /**
     * 단건 조회 — {@code company} 함께 fetch.
     * <p>본인 소유 검증·만료 검증·활성 검증은 모두 서비스 레이어 책임.</p>
     */
    @Query("""
            SELECT r FROM ChatRoom r
              JOIN FETCH r.company c
             WHERE r.roomId = :roomId
            """)
    Optional<ChatRoom> findByIdWithCompany(@Param("roomId") Long roomId);

    /**
     * 동일 (userSeq, corpCode) 에 속한 활성 방을 lastActiveAt 내림차순으로 조회.
     *
     * <p>SOFT 단일 활성 방 정책 — DB unique constraint 없이 race 케이스(잠시 활성 방 2개 이상 공존) 를
     * 흡수한다. 호출 측 서비스는 첫 항목(가장 최근)만 채택하고 나머지는 {@link ChatRoom#close} 처리.</p>
     *
     * <p>인덱스 {@code idx_room_user_active_last (user_seq, is_active, last_active_at DESC)} 가
     * (user_seq, is_active) prefix 매칭 + DESC 정렬을 그대로 활용. company_seq 필터링은 결과 집합이
     * 작아 부담 미미.</p>
     */
    @Query("""
            SELECT r FROM ChatRoom r
              JOIN FETCH r.company c
             WHERE r.user.userSeq = :userSeq
               AND c.corpCode = :corpCode
               AND r.isActive = true
             ORDER BY r.lastActiveAt DESC
            """)
    List<ChatRoom> findActiveByUserSeqAndCorpCode(@Param("userSeq") Long userSeq,
                                                  @Param("corpCode") String corpCode);

    /**
     * 만료 임계({@code lastActiveAt + 30분 < now}) 초과한 활성 방을 일괄 close 처리 — 스케줄러용.
     *
     * <p>{@code @Modifying} 으로 dirty checking 우회 → UPDATE 한 방으로 N+1 차단. 사용자 수가 늘어도
     * 행 수 비례 단일 쿼리라 부담 미미.</p>
     *
     * <p>{@code threshold} 는 호출 측에서 {@code now.minusMinutes(30)} 전달. 두 시각을 분리해 받는 이유는
     * 한 트랜잭션 안에서 "기준 시각" 과 "close 시각" 의 의미 분리를 명확히 하기 위함 (실제로는 같은 시각이지만 readability).</p>
     *
     * @return UPDATE 영향 행 수 (close 처리된 방 개수)
     */
    @Modifying
    @Query("""
            UPDATE ChatRoom r
               SET r.isActive = false, r.closedAt = :now
             WHERE r.isActive = true
               AND r.lastActiveAt < :threshold
            """)
    int closeExpiredActive(@Param("now") LocalDateTime now,
                           @Param("threshold") LocalDateTime threshold);
}
