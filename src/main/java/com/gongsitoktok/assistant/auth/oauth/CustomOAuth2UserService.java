/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/auth/oauth/CustomOAuth2UserService.java
 */
package com.gongsitoktok.assistant.auth.oauth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

/**
 * provider user-info 응답을 우리 도메인 principal({@link OAuth2UserPrincipal}) 로 변환하는 서비스 (제작요청 v6 §스텝 H-3).
 *
 * <h3>흐름</h3>
 * <ol>
 *     <li>{@link DefaultOAuth2UserService#loadUser(OAuth2UserRequest)} — Spring 이 provider 에 access token 으로 user-info 호출.</li>
 *     <li>{@link OAuth2UserInfo#of(String, java.util.Map)} 로 provider 별 응답 추상화.</li>
 *     <li>{@link OAuth2UserPrincipal} 로 래핑해 반환. 이후 {@code OAuth2LoginSuccessHandler} 가 본 principal 을 받아 upsert.</li>
 * </ol>
 *
 * <h3>SecurityConfig 결선</h3>
 * <p>본 클래스가 {@link OAuth2UserService} 빈으로 등록되면, {@code SecurityConfig} 의 {@code ObjectProvider} 가 자동으로
 * 결선해 {@code .userInfoEndpoint(u -> u.userService(this))} 효과를 낸다.</p>
 */
@Slf4j
@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User raw = delegate.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        try {
            OAuth2UserInfo info = OAuth2UserInfo.of(registrationId, raw.getAttributes());
            log.info("OAuth2 user-info 변환 완료: provider={}, providerId={}", info.oauthService(), info.providerId());
            return new OAuth2UserPrincipal(info);
        } catch (IllegalArgumentException ex) {
            // 지원하지 않는 provider — 부팅 시점 설정 누락일 가능성이 높다.
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("unsupported_provider", "지원하지 않는 OAuth provider: " + registrationId, null),
                    ex);
        } catch (IllegalStateException ex) {
            // user-info 응답 필드 구조 변경 — provider 사양이 바뀐 경우.
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("malformed_user_info", ex.getMessage(), null),
                    ex);
        }
    }
}
