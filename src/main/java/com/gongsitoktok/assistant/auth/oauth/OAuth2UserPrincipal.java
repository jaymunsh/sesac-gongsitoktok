/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/auth/oauth/OAuth2UserPrincipal.java
 */
package com.gongsitoktok.assistant.auth.oauth;

import com.gongsitoktok.assistant.user.entity.OauthService;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Spring Security 의 OAuth2 흐름에서 사용되는 principal — provider 응답 + 우리 도메인 구분값을 동봉.
 *
 * <p>{@link OAuth2User} 를 구현해 {@code SecurityContext} 에 그대로 들어갈 수 있다.
 * {@code OAuth2LoginSuccessHandler} 가 이 principal 로부터 {@link #providerId()} 와 {@link #oauthService()} 를 꺼내
 * {@code tb_user} upsert 를 수행한다.</p>
 */
public class OAuth2UserPrincipal implements OAuth2User {

    private static final Collection<GrantedAuthority> DEFAULT_AUTHORITIES =
            List.of(new SimpleGrantedAuthority("ROLE_USER"));

    private final OauthService oauthService;
    private final String providerId;
    private final String nickname;
    private final Map<String, Object> attributes;
    private final String nameAttributeKey;

    public OAuth2UserPrincipal(OAuth2UserInfo info) {
        this.oauthService = info.oauthService();
        this.providerId = info.providerId();
        this.nickname = info.nickname();
        this.attributes = info.attributes();
        this.nameAttributeKey = info.nameAttributeKey();
    }

    public OauthService oauthService() {
        return oauthService;
    }

    public String providerId() {
        return providerId;
    }

    public String nickname() {
        return nickname;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return DEFAULT_AUTHORITIES;
    }

    /**
     * Spring 이 OAuth2 사용자 식별자로 사용하는 값. provider 마다 다른 attribute 키를 반환한다.
     */
    @Override
    public String getName() {
        Object v = attributes.get(nameAttributeKey);
        return v == null ? providerId : String.valueOf(v);
    }
}
