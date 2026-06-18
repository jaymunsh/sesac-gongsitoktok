/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/user/entity/User.java
 */
package com.gongsitoktok.assistant.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 사용자 엔티티 — {@code tb_user}.
 *
 * <h3>식별자 이중 구조</h3>
 * <ul>
 *     <li>{@link #userSeq} (내부 PK) — 불변 시퀀스. 모든 FK 참조 대상. 외부 API/URL 에 노출 금지.</li>
 *     <li>{@link #userId}  (노출 ID) — 비즈니스 키. 탈퇴 시 {@code #dismiss_{epochMillis}} suffix 가 붙어 변형.</li>
 * </ul>
 *
 * <h3>탈퇴(Soft Delete) 정책</h3>
 * <ul>
 *     <li>row 삭제 금지. {@link #isActive} 를 {@code false} 로 전환 + {@link #withdrawnAt} 기록.</li>
 *     <li>{@code userId} 에 {@code #dismiss_{epochMillis}} suffix 부착 → 동일 원본 ID 재가입 시 유니크 충돌 회피.</li>
 *     <li>OAuth 가입자는 {@code providerId} 에도 동일 suffix 부착 → {@code (oauthService, providerId)} 복합 유니크 충돌 회피.</li>
 *     <li>{@code userId} 입력 정규식 {@code ^[a-z0-9]{4,20}$} 은 {@code #} 를 차단하므로,
 *         사용자가 dismiss 변형 ID 를 직접 입력해 우회·충돌 시도하는 것도 함께 방어된다.</li>
 * </ul>
 *
 * <h3>JWT 와의 통합</h3>
 * <p>JWT 의 {@code sub} 는 항상 {@code String.valueOf(userSeq)} 로 통일된다. {@code userId} 는 표시·로깅용 custom claim 이며,
 * dismiss 변형이 일어나도 {@code userSeq} 는 불변이므로 토큰 식별자가 안정적으로 유지된다.</p>
 *
 * <h3>비밀번호 정책</h3>
 * <ul>
 *     <li>OAuth 가입자는 {@code password} 가 {@code null} 이며, {@code /login} · {@code /users/me/password} 경로 진입이 차단된다 (§4-2 · §4-5).</li>
 *     <li>로컬 가입자의 비밀번호는 {@code BCryptPasswordEncoder} 로 해시(60자)되어 보관된다.</li>
 * </ul>
 */
@Entity
@Table(
        name = "tb_user",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_user_id", columnNames = "user_id"),
                @UniqueConstraint(name = "uk_user_oauth", columnNames = {"oauth_service", "provider_id"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    /**
     * 내부 불변 PK. 다른 모든 테이블의 FK 가 본 컬럼을 가리킨다.
     * <p>외부(API 응답·URL) 노출 금지.</p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_seq")
    private Long userSeq;

    /**
     * 사용자에게 노출되는 로그인/표시 ID. 탈퇴 시 {@code #dismiss_{epochMillis}} suffix 부착.
     * <p>length=150 — dismiss suffix 수용 마진 포함.</p>
     */
    @Column(name = "user_id", nullable = false, length = 150)
    private String userId;

    /**
     * BCrypt 해시(length 60). OAuth 가입자는 NULL.
     */
    @Column(name = "password", length = 60)
    private String password;

    /** 표시명. OAuth 가입자는 provider 가 제공한 이름을 초기값으로. */
    @Column(name = "nickname", nullable = false, length = 50)
    private String nickname;

    /** 가입 경로 enum. LOCAL/GOOGLE/KAKAO/NAVER. */
    @Enumerated(EnumType.STRING)
    @Column(name = "oauth_service", nullable = false, length = 20)
    private OauthService oauthService;

    /**
     * OAuth provider 발급 외부 식별자. 로컬 가입자는 NULL.
     * <p>탈퇴 시 {@code #dismiss_{epochMillis}} suffix 부착.</p>
     */
    @Column(name = "provider_id", length = 255)
    private String providerId;

    /** 활성 여부. 탈퇴 시 {@code false} 로 전환. 보호 경로 진입은 {@code true} 만 허용. */
    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    /** 가입 시각. Hibernate 가 자동 세팅. */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 탈퇴 시각. 미탈퇴 시 NULL. */
    @Column(name = "withdrawn_at")
    private LocalDateTime withdrawnAt;

    /**
     * 정적 팩토리 — 로컬 가입.
     *
     * @param userId         정규식 검증 통과 후의 노출 ID (4~20자, 영문 소문자+숫자)
     * @param encodedPassword BCrypt 해시
     * @param nickname       표시명
     */
    public static User createLocalUser(String userId, String encodedPassword, String nickname) {
        User u = new User();
        u.userId = userId;
        u.password = encodedPassword;
        u.nickname = nickname;
        u.oauthService = OauthService.LOCAL;
        u.providerId = null;
        u.isActive = true;
        return u;
    }

    /**
     * 정적 팩토리 — OAuth 가입.
     *
     * @param oauthService GOOGLE/KAKAO/NAVER
     * @param providerId   provider 발급 외부 식별자
     * @param nickname     provider 제공 표시명
     * @return 신규 OAuth 사용자 (userId 는 {@code "{provider}_{providerId}"} 형식)
     */
    public static User createOauthUser(OauthService oauthService, String providerId, String nickname) {
        if (oauthService == OauthService.LOCAL) {
            throw new IllegalArgumentException("createOauthUser 는 LOCAL 이 아닌 oauthService 만 허용한다.");
        }
        User u = new User();
        u.userId = oauthService.name().toLowerCase() + "_" + providerId;
        u.password = null;
        u.nickname = nickname;
        u.oauthService = oauthService;
        u.providerId = providerId;
        u.isActive = true;
        return u;
    }

    /**
     * 비밀번호 갱신 (LOCAL 사용자 전용).
     *
     * <p>호출 전 정책 검증과 동시성 보호(ReentrantLock) 는 서비스 레이어 책임이며, 본 메서드는 단순 상태 변경만 수행.</p>
     *
     * @param encodedNewPassword BCrypt 로 인코딩된 새 비밀번호
     */
    public void changePassword(String encodedNewPassword) {
        if (this.oauthService != OauthService.LOCAL) {
            throw new IllegalStateException("OAuth 사용자는 비밀번호를 가질 수 없다.");
        }
        this.password = encodedNewPassword;
    }

    /**
     * 표시명 변경.
     */
    public void changeNickname(String newNickname) {
        this.nickname = newNickname;
    }

    /**
     * 회원 탈퇴 처리 — soft delete + 식별자 dismiss 변형.
     *
     * <ol>
     *     <li>{@link #userId} ← {@code userId + "#dismiss_" + epochMillis}</li>
     *     <li>OAuth 사용자이면 {@link #providerId} ← {@code providerId + "#dismiss_" + epochMillis}</li>
     *     <li>{@link #isActive} ← {@code false}</li>
     *     <li>{@link #withdrawnAt} ← 현재 시각</li>
     * </ol>
     *
     * @param epochMillis 호출 시각 (System.currentTimeMillis), 외부에서 주입해 순수 변형 가능
     * @param now         탈퇴 기록 시각
     */
    public void markWithdrawn(long epochMillis, LocalDateTime now) {
        String suffix = "#dismiss_" + epochMillis;
        this.userId = this.userId + suffix;
        if (this.oauthService != OauthService.LOCAL && this.providerId != null) {
            this.providerId = this.providerId + suffix;
        }
        this.isActive = false;
        this.withdrawnAt = now;
    }

    /**
     * 빌더 — 테스트/리포지토리 내부용. 운영 코드는 정적 팩토리({@link #createLocalUser}, {@link #createOauthUser}) 사용 권장.
     */
    @Builder(access = AccessLevel.PACKAGE)
    private User(Long userSeq, String userId, String password, String nickname,
                 OauthService oauthService, String providerId, boolean isActive,
                 LocalDateTime createdAt, LocalDateTime withdrawnAt) {
        this.userSeq = userSeq;
        this.userId = userId;
        this.password = password;
        this.nickname = nickname;
        this.oauthService = oauthService;
        this.providerId = providerId;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.withdrawnAt = withdrawnAt;
    }
}
