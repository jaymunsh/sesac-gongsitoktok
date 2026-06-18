/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/auth/service/AuthService.java
 */
package com.gongsitoktok.assistant.auth.service;

import com.gongsitoktok.assistant.auth.dto.LoginRequest;
import com.gongsitoktok.assistant.auth.dto.SignupRequest;
import com.gongsitoktok.assistant.auth.service.AuthFacade.IssuedTokens;
import com.gongsitoktok.assistant.auth.validator.PasswordValidator;
import com.gongsitoktok.assistant.global.error.ErrorCode;
import com.gongsitoktok.assistant.global.error.exception.BusinessException;
import com.gongsitoktok.assistant.global.error.exception.UserWithdrawnException;
import com.gongsitoktok.assistant.user.entity.OauthService;
import com.gongsitoktok.assistant.user.entity.User;
import com.gongsitoktok.assistant.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.regex.Pattern;

/**
 * 로컬 회원가입 · 로그인 책임 서비스 (제작요청 v6 §4-1, §4-2).
 *
 * <h3>CPU 격리 — BCrypt 별도 풀</h3>
 * <p>{@link PasswordEncoder#encode(CharSequence)} / {@code matches} 는 가상 스레드 위에서 직접 실행하지 않고
 * {@code bcryptExecutor} ({@link com.gongsitoktok.assistant.global.config.BCryptAsyncConfig}) 로 위임 후
 * {@code CompletableFuture#join()} 으로 대기. carrier 스레드 점유 부담 차단.</p>
 *
 * <h3>userId 정책</h3>
 * <p>{@link #USER_ID_PATTERN} = {@code ^[a-z0-9]{4,20}$}. 정규식이 {@code #} 와 대문자·한글을 차단하므로 dismiss 변형 우회 시도도 방어.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Pattern USER_ID_PATTERN = Pattern.compile("^[a-z0-9]{4,20}$");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthFacade authFacade;

    @Qualifier("bcryptExecutor")
    private final ExecutorService bcryptExecutor;

    /**
     * 회원가입 (§4-1).
     *
     * <ol>
     *     <li>userId 정규식 검증 → 위반 시 {@code INVALID_USER_ID_FORMAT}.</li>
     *     <li>password 정책 검증 → 위반 시 {@code PASSWORD_POLICY_VIOLATION}.</li>
     *     <li>활성 사용자 중 중복 userId 확인 → 중복 시 {@code USER_ID_DUPLICATED}.</li>
     *     <li>BCrypt 해시 (bcryptExecutor 위임) → {@code tb_user} INSERT.</li>
     * </ol>
     *
     * @return 가입된 사용자의 외부 노출 ID
     */
    @Transactional
    public String signup(SignupRequest req) {
        if (!USER_ID_PATTERN.matcher(req.userId()).matches()) {
            throw new BusinessException(ErrorCode.INVALID_USER_ID_FORMAT);
        }
        if (!PasswordValidator.matchesPolicy(req.password())) {
            throw new BusinessException(ErrorCode.PASSWORD_POLICY_VIOLATION);
        }
        if (userRepository.existsByUserIdAndIsActiveTrue(req.userId())) {
            throw new BusinessException(ErrorCode.USER_ID_DUPLICATED);
        }

        String encoded = bcryptEncode(req.password());
        User saved = userRepository.save(User.createLocalUser(req.userId(), encoded, req.nickname()));
        log.info("회원가입 완료: userSeq={}, userId={}", saved.getUserSeq(), saved.getUserId());
        return saved.getUserId();
    }

    /**
     * 로컬 로그인 (§4-2).
     *
     * <ol>
     *     <li>활성 사용자 조회 — 미존재 또는 비활성이면 동일 401 ({@code INVALID_TOKEN}) — 존재 자체를 노출하지 않음.</li>
     *     <li>{@code oauthService != LOCAL} → {@code INVALID_LOGIN_METHOD}.</li>
     *     <li>BCrypt matches (bcryptExecutor) → 실패 시 401.</li>
     *     <li>{@link AuthFacade#issueLoginTokens(User, String)} 호출.</li>
     * </ol>
     */
    @Transactional
    public IssuedTokens login(LoginRequest req, String userAgent) {
        User user = userRepository.findByUserIdAndIsActiveTrue(req.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN, "아이디 또는 비밀번호가 올바르지 않습니다."));

        // 활성 검증은 쿼리에서 이미 걸렀지만 명세상 명시적 흐름 표현.
        if (!user.isActive()) {
            throw new UserWithdrawnException();
        }
        if (user.getOauthService() != OauthService.LOCAL) {
            throw new BusinessException(ErrorCode.INVALID_LOGIN_METHOD,
                    "이 계정은 소셜 로그인 전용입니다: " + user.getOauthService());
        }

        boolean matches = bcryptMatches(req.password(), user.getPassword());
        if (!matches) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        return authFacade.issueLoginTokens(user, userAgent);
    }

    /**
     * BCrypt 인코딩을 별도 풀에 위임 — 가상 스레드는 결과 대기만.
     */
    private String bcryptEncode(String raw) {
        return CompletableFuture.supplyAsync(() -> passwordEncoder.encode(raw), bcryptExecutor).join();
    }

    /**
     * BCrypt 검증을 별도 풀에 위임.
     */
    private boolean bcryptMatches(String raw, String encoded) {
        return CompletableFuture.supplyAsync(() -> passwordEncoder.matches(raw, encoded), bcryptExecutor).join();
    }
}
