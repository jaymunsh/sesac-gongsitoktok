/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/auth/repository/RefreshTokenRepository.java
 */
package com.gongsitoktok.assistant.auth.repository;

import com.gongsitoktok.assistant.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Refresh Token 리포지토리.
 *
 * <p>주의: 모든 조회는 {@code tokenHash} 기준이며, 원본 토큰은 절대 다루지 않는다 (보안 §스텝 H-4).</p>
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * 해시 기준 단건 조회 (rotation · revoke · 재사용 탐지).
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * 특정 유저의 활성 토큰을 일괄 revoke 처리 — 비밀번호 변경(§4-5), 회원 탈퇴(§4-6) 에서 사용.
     *
     * @param userSeq 사용자 PK
     * @param now     revoke 시각
     * @return 갱신된 row 수
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE RefreshToken rt
               SET rt.revokedAt = :now
             WHERE rt.user.userSeq = :userSeq
               AND rt.revokedAt IS NULL
            """)
    int revokeAllByUserSeq(@Param("userSeq") Long userSeq, @Param("now") LocalDateTime now);
}
