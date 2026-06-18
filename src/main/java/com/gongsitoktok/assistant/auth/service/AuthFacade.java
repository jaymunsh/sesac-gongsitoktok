/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/auth/service/AuthFacade.java
 */
package com.gongsitoktok.assistant.auth.service;

import com.gongsitoktok.assistant.auth.jwt.JwtTokenProvider;
import com.gongsitoktok.assistant.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Access Token + Refresh Token 동시 발급의 통합 진입점 (제작요청 v6 §스텝 H-4).
 *
 * <p>로컬 로그인({@code AuthService.login}) · OAuth 로그인({@code OAuth2LoginSuccessHandler}) 양쪽이 공통으로 호출하여
 * 발급 정책의 분기를 없앤다.</p>
 */
@Component
@RequiredArgsConstructor
public class AuthFacade {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    @Value("${jwt.access-token-validity-seconds}")
    private long accessTokenValiditySeconds;

    /**
     * Access + Refresh 동시 발급.
     *
     * @param user      검증된 사용자 (LOCAL 이면 password 검증 완료, OAuth 면 upsert 완료)
     * @param userAgent 요청 헤더 User-Agent
     * @return 발급 결과 — 컨트롤러가 응답 쿠키·바디로 분리 노출
     */
    public IssuedTokens issueLoginTokens(User user, String userAgent) {
        String accessToken = jwtTokenProvider.issueAccessToken(
                user.getUserSeq(), user.getUserId(), user.getOauthService());
        String refreshTokenRaw = refreshTokenService.issue(user, userAgent);
        return new IssuedTokens(accessToken, refreshTokenRaw, accessTokenValiditySeconds);
    }

    /**
     * @param accessToken           응답 body 의 Access Token
     * @param refreshTokenRaw       응답 Set-Cookie 의 Refresh Token (원본, base64url)
     * @param accessExpiresInSeconds Access Token 만료 초
     */
    public record IssuedTokens(String accessToken, String refreshTokenRaw, long accessExpiresInSeconds) {
    }
}
