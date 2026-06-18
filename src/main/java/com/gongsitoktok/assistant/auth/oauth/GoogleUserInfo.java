/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/auth/oauth/GoogleUserInfo.java
 */
package com.gongsitoktok.assistant.auth.oauth;

import com.gongsitoktok.assistant.user.entity.OauthService;

import java.util.Map;

/**
 * Google OIDC user-info 응답 — {@code {sub, name, email, picture, ...}}.
 *
 * <p>{@code sub} 는 ID Token 의 표준 클레임이며 provider 영구 식별자. 이메일은 변경 가능하므로 사용하지 않는다.</p>
 */
public record GoogleUserInfo(Map<String, Object> attributes) implements OAuth2UserInfo {

    @Override
    public OauthService oauthService() {
        return OauthService.GOOGLE;
    }

    @Override
    public String providerId() {
        Object sub = attributes.get("sub");
        if (sub == null) {
            throw new IllegalStateException("Google user-info 응답에 sub 필드가 없습니다.");
        }
        return String.valueOf(sub);
    }

    @Override
    public String nickname() {
        Object name = attributes.get("name");
        return name == null ? "Google 사용자" : String.valueOf(name);
    }

    @Override
    public String nameAttributeKey() {
        return "sub";
    }
}
