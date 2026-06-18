/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/auth/oauth/KakaoUserInfo.java
 */
package com.gongsitoktok.assistant.auth.oauth;

import com.gongsitoktok.assistant.user.entity.OauthService;

import java.util.Map;

/**
 * Kakao user-info 응답 — {@code { id, properties: { nickname }, kakao_account: { email } }}.
 *
 * <p>{@code id} 는 Long (숫자) 이지만 String 으로 정규화하여 {@code tb_user.provider_id} 에 저장한다.
 * Kakao 는 동의항목 설정에 따라 {@code properties} / {@code kakao_account} 가 비어 있을 수 있으므로 안전 fallback.</p>
 */
public record KakaoUserInfo(Map<String, Object> attributes) implements OAuth2UserInfo {

    @Override
    public OauthService oauthService() {
        return OauthService.KAKAO;
    }

    @Override
    public String providerId() {
        Object id = attributes.get("id");
        if (id == null) {
            throw new IllegalStateException("Kakao user-info 응답에 id 필드가 없습니다.");
        }
        return String.valueOf(id);
    }

    @Override
    @SuppressWarnings("unchecked")
    public String nickname() {
        Object properties = attributes.get("properties");
        if (properties instanceof Map<?, ?> m) {
            Object n = ((Map<String, Object>) m).get("nickname");
            if (n != null) {
                return String.valueOf(n);
            }
        }
        return "Kakao 사용자";
    }

    @Override
    public String nameAttributeKey() {
        return "id";
    }
}
