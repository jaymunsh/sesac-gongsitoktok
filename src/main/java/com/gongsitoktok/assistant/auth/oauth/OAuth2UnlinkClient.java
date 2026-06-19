/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/auth/oauth/OAuth2UnlinkClient.java
 */
package com.gongsitoktok.assistant.auth.oauth;

import com.gongsitoktok.assistant.global.error.ErrorCode;
import com.gongsitoktok.assistant.global.error.exception.BusinessException;
import com.gongsitoktok.assistant.user.entity.OauthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;

/**
 * OAuth provider 측 연동 해제 (Google revoke · Kakao unlink · Naver delete) 호출 클라이언트.
 *
 * <h3>호출 시점</h3>
 * <p>{@code UserService.withdraw} 가 OAuth 사용자에 한해 호출. LOCAL 사용자는 분기 자체를 안 탄다.</p>
 *
 * <h3>실패 정책 — Strict (provider 실패 = 우리 측 탈퇴도 실패)</h3>
 * <ul>
 *     <li>provider 측 호출이 실패하면 {@link BusinessException}({@link ErrorCode#OAUTH_UNLINK_FAILED}) 으로 변환하여 throw.</li>
 *     <li>호출자({@code UserService.withdraw}) 는 {@code @Transactional} 이므로 예외 발생 시 자동 롤백 →
 *         {@code tb_user} 의 dismiss 변형·{@code isActive=false} 처리가 적용되지 않는다.</li>
 *     <li>사용자에게는 {@code 502 OAUTH_UNLINK_FAILED} 응답이 반환되어 재시도를 유도.</li>
 * </ul>
 *
 * <h3>실패로 간주되는 케이스</h3>
 * <ul>
 *     <li>access token 미보유 (메모리에서 만료·증발 등) — provider 식별 자체가 불가</li>
 *     <li>Naver: ClientRegistration 미등록 (application.yml 누락)</li>
 *     <li>provider 측 4xx/5xx 응답</li>
 *     <li>네트워크 IO 오류 / 10초 타임아웃</li>
 * </ul>
 *
 * <h3>provider 별 사양</h3>
 * <ul>
 *     <li><b>Google</b>: {@code POST https://oauth2.googleapis.com/revoke?token=ACCESS_TOKEN} (token 만 필요)</li>
 *     <li><b>Kakao</b>: {@code POST https://kapi.kakao.com/v1/user/unlink} + {@code Authorization: Bearer ACCESS_TOKEN}</li>
 *     <li><b>Naver</b>: {@code GET https://nid.naver.com/oauth2.0/token?grant_type=delete&client_id=...&client_secret=...&access_token=...}
 *         (client 인증 정보까지 필요 — {@code ClientRegistrationRepository} 에서 추출)</li>
 * </ul>
 *
 * <h3>WebClient 인스턴스</h3>
 * <p>FastAPI 호출용({@code fastApiWebClient}) 과 분리된 generic 인스턴스를 본 클래스 내부에 보관.
 * 외부 도메인 호출이므로 baseUrl 미설정. 응답 크기 64KB 마진 (provider 응답은 짧음).</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2UnlinkClient {

    private static final URI KAKAO_UNLINK_URI = URI.create("https://kapi.kakao.com/v1/user/unlink");
    private static final String NAVER_TOKEN_HOST = "nid.naver.com";
    private static final Duration CALL_TIMEOUT = Duration.ofSeconds(10);

    private final ClientRegistrationRepository clientRegistrationRepository;

    private final WebClient webClient = WebClient.builder()
            .codecs(c -> c.defaultCodecs().maxInMemorySize(64 * 1024))
            .build();

    /**
     * provider 별 unlink 호출. 실패 시 {@link BusinessException}({@link ErrorCode#OAUTH_UNLINK_FAILED}) throw.
     *
     * @param provider    OAuth provider (LOCAL 은 정상 no-op)
     * @param accessToken provider 측 access token (null 또는 빈 값이면 실패로 간주)
     * @return 성공 시 정상 완결된 {@code Mono<Void>}, 실패 시 {@code Mono.error(BusinessException)}
     */
    public Mono<Void> unlink(OauthService provider, String accessToken) {
        if (provider == null || provider == OauthService.LOCAL) {
            return Mono.empty();
        }
        if (accessToken == null || accessToken.isBlank()) {
            return Mono.error(new BusinessException(ErrorCode.OAUTH_UNLINK_FAILED,
                    "OAuth access token 미보유 — provider=" + provider + " 측 연동 해제 호출 불가"));
        }
        return switch (provider) {
            case GOOGLE -> revokeGoogle(accessToken);
            case KAKAO -> unlinkKakao(accessToken);
            case NAVER -> deleteNaver(accessToken);
            case LOCAL -> Mono.empty();
        };
    }

    private Mono<Void> revokeGoogle(String token) {
        return webClient.post()
                .uri(uri -> uri.scheme("https").host("oauth2.googleapis.com").path("/revoke")
                        .queryParam("token", token).build())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .retrieve()
                .bodyToMono(Void.class)
                .timeout(CALL_TIMEOUT)
                .doOnSuccess(v -> log.info("Google OAuth revoke 성공"))
                .onErrorMap(ex -> toBusinessException("Google", ex));
    }

    private Mono<Void> unlinkKakao(String token) {
        return webClient.post()
                .uri(KAKAO_UNLINK_URI)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(Void.class)
                .timeout(CALL_TIMEOUT)
                .doOnSuccess(v -> log.info("Kakao OAuth unlink 성공"))
                .onErrorMap(ex -> toBusinessException("Kakao", ex));
    }

    private Mono<Void> deleteNaver(String token) {
        ClientRegistration naver = clientRegistrationRepository.findByRegistrationId("naver");
        if (naver == null) {
            return Mono.error(new BusinessException(ErrorCode.OAUTH_UNLINK_FAILED,
                    "Naver ClientRegistration 미등록 — unlink 호출 불가"));
        }
        String clientId = naver.getClientId();
        String clientSecret = naver.getClientSecret();
        return webClient.get()
                .uri(uri -> uri.scheme("https").host(NAVER_TOKEN_HOST).path("/oauth2.0/token")
                        .queryParam("grant_type", "delete")
                        .queryParam("client_id", clientId)
                        .queryParam("client_secret", clientSecret)
                        .queryParam("access_token", token)
                        .queryParam("service_provider", "NAVER")
                        .build())
                .retrieve()
                .bodyToMono(Void.class)
                .timeout(CALL_TIMEOUT)
                .doOnSuccess(v -> log.info("Naver OAuth delete 성공"))
                .onErrorMap(ex -> toBusinessException("Naver", ex));
    }

    /**
     * 이미 {@link BusinessException} 이면 그대로 전파 (Naver ClientRegistration 미등록 같은 케이스).
     * 그 외 모든 예외(4xx/5xx/타임아웃/IO) 는 {@link ErrorCode#OAUTH_UNLINK_FAILED} 로 변환.
     */
    private Throwable toBusinessException(String providerLabel, Throwable cause) {
        if (cause instanceof BusinessException be) {
            return be;
        }
        log.warn("{} OAuth unlink 실패: {}", providerLabel, cause.getMessage());
        return new BusinessException(ErrorCode.OAUTH_UNLINK_FAILED,
                providerLabel + " 연동 해제 호출 실패: " + cause.getMessage(), cause);
    }
}
