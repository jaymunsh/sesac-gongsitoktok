/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/user/repository/UserRepository.java
 */
package com.gongsitoktok.assistant.user.repository;

import com.gongsitoktok.assistant.user.entity.OauthService;
import com.gongsitoktok.assistant.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 사용자 리포지토리.
 *
 * <p>조회 메서드 명명 규칙:</p>
 * <ul>
 *     <li>{@code findActive...} — {@code isActive=true} 만 반환. 인증/인가 흐름의 기본 경로.</li>
 *     <li>{@code existsByUserId} — 회원가입 중복 사전 확인.</li>
 * </ul>
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 노출 ID(`userId`) 로 활성 사용자 조회 (로그인·중복 확인 분기).
     */
    Optional<User> findByUserIdAndIsActiveTrue(String userId);

    /**
     * 회원가입 중복 사전 확인.
     */
    boolean existsByUserIdAndIsActiveTrue(String userId);

    /**
     * OAuth provider + providerId 로 활성 사용자 조회.
     * <p>dismiss 된 행은 providerId 에 suffix 가 붙어 있으므로 자연스럽게 조회 대상에서 제외된다.</p>
     */
    Optional<User> findByOauthServiceAndProviderIdAndIsActiveTrue(OauthService oauthService, String providerId);
}
