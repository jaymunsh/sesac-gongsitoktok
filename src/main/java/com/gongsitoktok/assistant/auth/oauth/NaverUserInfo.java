/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/auth/oauth/NaverUserInfo.java
 */
package com.gongsitoktok.assistant.auth.oauth;

import com.gongsitoktok.assistant.user.entity.OauthService;

import java.util.Map;

/**
 * Naver user-info 응답 — {@code { response: { id, name, email, ... } }}.
 *
 * <p>모든 사용자 데이터가 {@code response} 키 안에 한 번 더 감싸져 있다. {@code attributes} 자체는 원본을 유지하되,
 * {@link #providerId()} / {@link #nickname()} 은 unwrap 해서 반환한다.</p>
 */
public record NaverUserInfo(Map<String, Object> attributes) implements OAuth2UserInfo {

    @Override
    public OauthService oauthService() {
        return OauthService.NAVER;
    }

    @Override
    public String providerId() {
        return String.valueOf(response().get("id"));
    }

    @Override
    public String nickname() {
        Object name = response().get("name");
        return name == null ? "Naver 사용자" : String.valueOf(name);
    }

    @Override
    public String nameAttributeKey() {
        return "response";
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> response() {
        Object r = attributes.get("response");
        if (!(r instanceof Map<?, ?>)) {
            throw new IllegalStateException("Naver user-info 응답에 response 필드가 없습니다.");
        }
        return (Map<String, Object>) r;
    }
}
