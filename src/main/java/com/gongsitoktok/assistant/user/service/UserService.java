/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/user/service/UserService.java
 */
package com.gongsitoktok.assistant.user.service;

import com.gongsitoktok.assistant.auth.jwt.JwtTokenProvider;
import com.gongsitoktok.assistant.auth.service.RefreshTokenService;
import com.gongsitoktok.assistant.auth.validator.PasswordValidator;
import com.gongsitoktok.assistant.global.error.ErrorCode;
import com.gongsitoktok.assistant.global.error.exception.BusinessException;
import com.gongsitoktok.assistant.user.dto.PasswordChangeRequest;
import com.gongsitoktok.assistant.user.dto.UserMeResponse;
import com.gongsitoktok.assistant.user.dto.WithdrawResponse;
import com.gongsitoktok.assistant.user.entity.OauthService;
import com.gongsitoktok.assistant.user.entity.User;
import com.gongsitoktok.assistant.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 사용자 서비스 — 마이페이지 조회 · 비밀번호 변경 · 회원 탈퇴 (제작요청 v6 §4-4, §4-5, §4-6).
 *
 * <h3>비밀번호 변경의 동시성 보호</h3>
 * <ul>
 *     <li>같은 사용자에 대한 동시 비밀번호 변경 요청을 직렬화하기 위해 {@link ReentrantLock} 을 사용한다.</li>
 *     <li>구형 {@code synchronized} 를 회피하는 이유: 가상 스레드가 {@code synchronized} 블록 안에서 IO 대기를 하면
 *         carrier 스레드를 붙잡는 Pinned Thread 가 발생한다. {@link ReentrantLock} 은 park/unpark 기반이라 안전.</li>
 *     <li>락은 {@code userSeq} 단위로 분리 — 다른 사용자의 변경은 서로 블록하지 않음.</li>
 *     <li>락 객체는 약 참조 정리를 위해 변경 종료 시 {@link ConcurrentHashMap} 에서 제거 (홀더가 없을 때만).</li>
 * </ul>
 *
 * <h3>CPU 격리</h3>
 * <p>BCrypt encode/matches 는 {@code bcryptExecutor} 로 위임 (§스텝 F).</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final JwtTokenProvider jwtTokenProvider;

    @Qualifier("bcryptExecutor")
    private final ExecutorService bcryptExecutor;

    private final ConcurrentHashMap<Long, ReentrantLock> userLocks = new ConcurrentHashMap<>();

    /**
     * 마이페이지 조회.
     */
    @Transactional(readOnly = true)
    public UserMeResponse me(Long userSeq) {
        User user = loadActive(userSeq);
        return UserMeResponse.from(user);
    }

    /**
     * 비밀번호 변경 (LOCAL 전용).
     *
     * @param userSeq            현재 인증된 사용자 PK
     * @param req                현재/새 비밀번호
     * @param currentAccessToken 현재 호출에 사용된 Access Token (블랙리스트 등록용)
     */
    public void changePassword(Long userSeq, PasswordChangeRequest req, String currentAccessToken) {
        ReentrantLock lock = userLocks.computeIfAbsent(userSeq, k -> new ReentrantLock());
        lock.lock();
        try {
            doChangePassword(userSeq, req, currentAccessToken);
        } finally {
            lock.unlock();
            // 사용 종료 시 락 객체 정리 — 다른 대기자가 없을 때만.
            userLocks.compute(userSeq, (k, existing) -> {
                if (existing != null && !existing.hasQueuedThreads() && !existing.isLocked()) {
                    return null;
                }
                return existing;
            });
        }
    }

    @Transactional
    protected void doChangePassword(Long userSeq, PasswordChangeRequest req, String currentAccessToken) {
        User user = loadActive(userSeq);
        if (user.getOauthService() != OauthService.LOCAL) {
            throw new BusinessException(ErrorCode.OAUTH_USER_PASSWORD_CHANGE_DENIED);
        }
        boolean ok = bcryptMatches(req.currentPassword(), user.getPassword());
        if (!ok) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "현재 비밀번호가 올바르지 않습니다.");
        }
        if (!PasswordValidator.matchesPolicy(req.newPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_POLICY_VIOLATION);
        }

        String encoded = bcryptEncode(req.newPassword());
        user.changePassword(encoded);

        // 다중 디바이스 일괄 무효화 + 현재 Access Token 블랙리스트
        refreshTokenService.revokeAllByUser(userSeq);
        blacklistCurrentToken(currentAccessToken);
        log.info("비밀번호 변경 완료: userSeq={}", userSeq);
    }

    /**
     * 회원 탈퇴 (§4-6) — soft delete + 식별자 dismiss 변형.
     *
     * @return 신규 탈퇴면 {@code alreadyWithdrawn=false}, 멱등이면 {@code true}.
     */
    @Transactional
    public WithdrawResponse withdraw(Long userSeq, String currentAccessToken) {
        User user = userRepository.findById(userSeq)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));

        if (!user.isActive()) {
            return new WithdrawResponse(user.getWithdrawnAt(), true);
        }

        long epoch = System.currentTimeMillis();
        LocalDateTime now = LocalDateTime.now();
        user.markWithdrawn(epoch, now);

        refreshTokenService.revokeAllByUser(userSeq);
        blacklistCurrentToken(currentAccessToken);

        log.info("회원 탈퇴 처리: userSeq={}, dismissed userId={}", userSeq, user.getUserId());
        return new WithdrawResponse(now, false);
    }

    // ===== 내부 =====

    private User loadActive(Long userSeq) {
        User user = userRepository.findById(userSeq)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN, "사용자를 찾을 수 없습니다."));
        if (!user.isActive()) {
            throw new BusinessException(ErrorCode.USER_WITHDRAWN);
        }
        return user;
    }

    private void blacklistCurrentToken(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        Claims claims = jwtTokenProvider.parseAndValidate(token);
        jwtTokenProvider.blacklist(claims.getId(), jwtTokenProvider.extractExpiryEpochMillis(claims));
        jwtTokenProvider.invalidateClaimsCache(token);
    }

    private String bcryptEncode(String raw) {
        return CompletableFuture.supplyAsync(() -> passwordEncoder.encode(raw), bcryptExecutor).join();
    }

    private boolean bcryptMatches(String raw, String encoded) {
        return CompletableFuture.supplyAsync(() -> passwordEncoder.matches(raw, encoded), bcryptExecutor).join();
    }
}
