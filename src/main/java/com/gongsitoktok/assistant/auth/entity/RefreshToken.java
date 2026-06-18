/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/auth/entity/RefreshToken.java
 */
package com.gongsitoktok.assistant.auth.entity;

import com.gongsitoktok.assistant.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Refresh Token 저장 엔티티 — {@code tb_refresh_token}.
 *
 * <h3>보안 모델</h3>
 * <ul>
 *     <li>원본 토큰(랜덤 256bit, base64url) 은 DB 에 절대 저장하지 않음. 클라이언트의 httpOnly 쿠키에만 존재.</li>
 *     <li>DB 에는 {@link #tokenHash} (SHA-256 hex 64자) 만 저장 → DB 유출 시에도 토큰 도용 불가.</li>
 *     <li>한 사용자가 다중 디바이스에 로그인할 수 있으므로 1:N 관계 ({@code user_seq} 기준).</li>
 *     <li>비밀번호 변경·회원 탈퇴 시 해당 유저의 모든 행을 일괄 revoke → 다중 디바이스 일괄 무효화.</li>
 * </ul>
 *
 * <h3>FK 안정성</h3>
 * <p>FK 는 항상 불변 {@code tb_user.user_seq} 를 참조. 사용자가 탈퇴(soft delete)되어 {@code userId} 가 dismiss 변형되어도
 * FK 는 깨지지 않는다. cascade 명시 없음.</p>
 *
 * <h3>인덱스</h3>
 * <p>{@code (user_seq, revoked_at, expires_at)} 복합 인덱스 — 특정 유저의 활성 토큰 조회(rotation 시) 및 일괄 revoke 쿼리 가속.</p>
 */
@Entity
@Table(
        name = "tb_refresh_token",
        indexes = {
                @Index(name = "idx_refresh_user_active", columnList = "user_seq, revoked_at, expires_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "token_id")
    private Long tokenId;

    /**
     * FK → {@code tb_user.user_seq} (불변). cascade 없음.
     * <p>{@code FetchType.LAZY} — Refresh 검증 시 사용자 row 까지 즉시 로드할 필요가 없음.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_seq", referencedColumnName = "user_seq", nullable = false,
            foreignKey = @jakarta.persistence.ForeignKey(name = "fk_refresh_token_user"))
    private User user;

    /**
     * Refresh Token 원본의 SHA-256 해시(hex 64자). unique 제약.
     */
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    /** 발급 후 14일 만료. */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /** 로그아웃·비밀번호 변경·탈퇴·rotation 시 세팅. NULL = 유효. */
    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    /** 발급 시 User-Agent (감사·다중 디바이스 식별). */
    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 정적 팩토리 — 신규 발급.
     *
     * @param user      소유 사용자 (불변 userSeq 참조)
     * @param tokenHash 원본 토큰의 SHA-256 hex
     * @param expiresAt 만료 시각 (now + 14d)
     * @param userAgent 요청 헤더 User-Agent (nullable)
     */
    public static RefreshToken issue(User user, String tokenHash, LocalDateTime expiresAt, String userAgent) {
        RefreshToken t = new RefreshToken();
        t.user = user;
        t.tokenHash = tokenHash;
        t.expiresAt = expiresAt;
        t.userAgent = userAgent;
        t.revokedAt = null;
        return t;
    }

    /**
     * revoke 처리. 이미 revoke 된 경우 멱등.
     *
     * @param now revoke 시각
     */
    public void revoke(LocalDateTime now) {
        if (this.revokedAt == null) {
            this.revokedAt = now;
        }
    }

    /**
     * 현재 유효 여부 — {@code revokedAt IS NULL AND expiresAt > now}.
     */
    public boolean isValid(LocalDateTime now) {
        return this.revokedAt == null && this.expiresAt.isAfter(now);
    }
}
