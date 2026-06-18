/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/auth/jwt/JwtTokenProvider.java
 */
package com.gongsitoktok.assistant.auth.jwt;

import com.gongsitoktok.assistant.global.error.ErrorCode;
import com.gongsitoktok.assistant.global.error.exception.BusinessException;
import com.gongsitoktok.assistant.user.entity.OauthService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Access Token 발급·검증 책임 빈 (제작요청 v6 §스텝 F · §스텝 H-4).
 *
 * <h3>토큰 페이로드</h3>
 * <ul>
 *     <li>{@code sub}            — {@code String.valueOf(userSeq)} (불변 내부 PK)</li>
 *     <li>{@code jti}            — {@link UUID#randomUUID()} (블랙리스트 키)</li>
 *     <li>{@code iat}, {@code exp} — 발급/만료 시각 (만료=1시간)</li>
 *     <li>{@code iss}            — {@code jwt.issuer} 설정값</li>
 *     <li>{@code userId}         — custom claim (표시·로깅)</li>
 *     <li>{@code oauthService}   — custom claim (분기 정책: 비밀번호 변경 차단 등)</li>
 * </ul>
 *
 * <h3>두 가지 캐시</h3>
 * <ol>
 *     <li><b>{@code jwtClaimsCache}</b> (3분 TTL, 최대 10,000개) — 검증 완료된 클레임. 화면 진입 시 burst 요청을 무거운
 *         서명 파싱 없이 캐시 hit 로 흡수.</li>
 *     <li><b>{@code jwtBlacklistCache}</b> (TTL = 토큰 잔여 만료 시간) — 로그아웃·비밀번호 변경·탈퇴 시 등록. Caffeine 의
 *         {@link Expiry} SPI 로 키별 동적 TTL 을 지원.</li>
 * </ol>
 *
 * <h3>검증 순서 (성능 + 보안)</h3>
 * <ol>
 *     <li><b>블랙리스트 hit</b> → 즉시 거부 ({@link BusinessException} {@link ErrorCode#INVALID_TOKEN}).</li>
 *     <li><b>클레임 캐시 hit</b> → 그대로 반환 (서명 파싱 생략).</li>
 *     <li><b>캐시 miss</b> → JJWT 로 서명 파싱·만료 검증 → 캐시 적재.</li>
 * </ol>
 *
 * <p>{@link JwtAuthenticationFilter} 가 이 순서를 그대로 호출해야 한다.</p>
 *
 * <h3>가상 스레드와의 상호작용</h3>
 * <p>JJWT 의 서명 파싱은 CPU 작업이지만 HMAC-SHA 는 BCrypt 와 달리 마이크로초 수준이라 가상 스레드 위에서 실행해도
 * carrier 점유 부담이 없다. 따라서 별도 풀 격리는 하지 않는다.</p>
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long validitySeconds;
    private final String issuer;

    private final Cache<String, Claims> jwtClaimsCache;
    private final Cache<String, Long> jwtBlacklistCache;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-validity-seconds}") long validitySeconds,
            @Value("${jwt.issuer}") String issuer
    ) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("jwt.secret 은 32바이트(256bit) 이상이어야 합니다. (현재 길이: " + keyBytes.length + ")");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.validitySeconds = validitySeconds;
        this.issuer = issuer;

        // 검증 캐시 — 3분 TTL, 최대 10,000개. burst 요청 흡수가 목적이라 짧은 TTL 충분.
        this.jwtClaimsCache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(3))
                .maximumSize(10_000)
                .build();

        // 블랙리스트 캐시 — 키별 동적 TTL (토큰 잔여 만료 시간만큼만 유지).
        // 잔여 시간 0 이하인 토큰은 즉시 expire (어차피 서명 파싱 단계에서 ExpiredJwtException 으로 걸림).
        this.jwtBlacklistCache = Caffeine.newBuilder()
                .expireAfter(new Expiry<String, Long>() {
                    @Override
                    public long expireAfterCreate(@NonNull String jti, @NonNull Long expiryEpochMillis, long currentTime) {
                        long remainMs = Math.max(0L, expiryEpochMillis - System.currentTimeMillis());
                        return TimeUnit.MILLISECONDS.toNanos(remainMs);
                    }

                    @Override
                    public long expireAfterUpdate(@NonNull String jti, @NonNull Long expiryEpochMillis,
                                                  long currentTime, long currentDuration) {
                        return currentDuration;
                    }

                    @Override
                    public long expireAfterRead(@NonNull String jti, @NonNull Long expiryEpochMillis,
                                                long currentTime, long currentDuration) {
                        return currentDuration;
                    }
                })
                .maximumSize(100_000)
                .build();
    }

    /**
     * Access Token 발급.
     *
     * @param userSeq      사용자 PK
     * @param userId       표시 ID
     * @param oauthService 가입 경로
     * @return JWT 직렬화 문자열
     */
    public String issueAccessToken(Long userSeq, String userId, OauthService oauthService) {
        Instant now = Instant.now();
        Date issuedAt = Date.from(now);
        Date expiresAt = Date.from(now.plusSeconds(validitySeconds));
        return Jwts.builder()
                .subject(String.valueOf(userSeq))
                .id(UUID.randomUUID().toString())
                .issuer(issuer)
                .issuedAt(issuedAt)
                .expiration(expiresAt)
                .claim("userId", userId)
                .claim("oauthService", oauthService.name())
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * 토큰 검증 — 블랙리스트 → 캐시 → 서명 파싱 순서.
     *
     * @param token Bearer 헤더에서 추출한 raw JWT
     * @return 검증된 {@link Claims}
     * @throws BusinessException {@link ErrorCode#INVALID_TOKEN} (블랙리스트/시그니처/포맷 오류)
     */
    public Claims parseAndValidate(String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "토큰이 비어 있습니다.");
        }

        // 1. 블랙리스트 hit 즉시 거부 (서명 파싱 없이)
        //    먼저 캐시 hit 으로 jti 가 확보된 경우엔 그것을 우선 사용. 캐시 miss 라면 서명 파싱 결과로 검사.
        Claims cached = jwtClaimsCache.getIfPresent(token);
        if (cached != null) {
            if (isBlacklisted(cached.getId())) {
                throw new BusinessException(ErrorCode.INVALID_TOKEN, "블랙리스트 처리된 토큰입니다.");
            }
            return cached;
        }

        // 2. 서명 파싱 + 만료 검증
        Claims fresh;
        try {
            fresh = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(issuer)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException ex) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "토큰이 만료되었습니다.", ex);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "토큰 검증 실패: " + ex.getMessage(), ex);
        }

        // 3. 블랙리스트 재검사 (캐시에 적재하기 전에)
        if (isBlacklisted(fresh.getId())) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "블랙리스트 처리된 토큰입니다.");
        }

        jwtClaimsCache.put(token, fresh);
        return fresh;
    }

    /**
     * jti 를 블랙리스트에 등록. TTL 은 토큰의 잔여 만료 시간과 일치.
     *
     * @param jti               토큰 ID
     * @param expiryEpochMillis 토큰 만료 epoch millis
     */
    public void blacklist(String jti, long expiryEpochMillis) {
        if (jti == null) {
            return;
        }
        jwtBlacklistCache.put(jti, expiryEpochMillis);
    }

    /**
     * 검증 캐시에서 단일 토큰 무효화 (비밀번호 변경 시 자신의 토큰 캐시 정리 용도).
     */
    public void invalidateClaimsCache(String token) {
        if (token != null) {
            jwtClaimsCache.invalidate(token);
        }
    }

    /**
     * {@code Claims#getSubject()} 를 Long 으로 변환. dismiss 변형과 무관하게 항상 안정.
     */
    public Long extractUserSeq(Claims claims) {
        try {
            return Long.parseLong(claims.getSubject());
        } catch (NumberFormatException ex) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN, "토큰 sub 가 숫자가 아닙니다.", ex);
        }
    }

    /**
     * custom claim {@code userId} 추출. 토큰 발급 이후 dismiss 변형되었더라도 발급 당시 값 그대로.
     */
    public String extractUserId(Claims claims) {
        return claims.get("userId", String.class);
    }

    /**
     * custom claim {@code oauthService} 를 enum 으로 변환.
     */
    public OauthService extractOauthService(Claims claims) {
        String name = claims.get("oauthService", String.class);
        return name == null ? OauthService.LOCAL : OauthService.valueOf(name);
    }

    /**
     * 토큰의 만료 시각(epoch millis) 을 반환. 블랙리스트 TTL 계산에 사용.
     */
    public long extractExpiryEpochMillis(Claims claims) {
        Date exp = claims.getExpiration();
        return exp == null ? System.currentTimeMillis() : exp.getTime();
    }

    private boolean isBlacklisted(String jti) {
        return jti != null && jwtBlacklistCache.getIfPresent(jti) != null;
    }
}
