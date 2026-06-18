/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/auth/oauth/OAuth2UserInfo.java
 */
package com.gongsitoktok.assistant.auth.oauth;

import com.gongsitoktok.assistant.user.entity.OauthService;

import java.util.Map;

/**
 * provider 별 user-info 응답 구조 차이를 흡수하는 추상화 (제작요청 v6 §스텝 H-3).
 *
 * <p>구현체는 {@link GoogleUserInfo} / {@link KakaoUserInfo} / {@link NaverUserInfo} 세 종.
 * 각각의 응답 attribute 키와 nesting 구조를 캡슐화한다.</p>
 */
public interface OAuth2UserInfo {

    /** {@link OauthService} 값. 도메인 매핑·DB upsert 키. */
    OauthService oauthService();

    /** provider 발급 외부 식별자. {@code tb_user.provider_id} 컬럼 값. */
    String providerId();

    /** provider 가 제공한 표시명. {@code tb_user.nickname} 초기값. */
    String nickname();

    /** 원본 attribute 맵 — {@code OAuth2User#getAttributes()} 반환용. */
    Map<String, Object> attributes();

    /** {@code OAuth2User#getName()} 이 반환할 키. provider 마다 상이. */
    String nameAttributeKey();

    /**
     * 정적 팩토리 — registrationId 로 적절한 구현체 선택.
     *
     * @param registrationId Spring Security OAuth2 의 registration ID (google/kakao/naver)
     * @param attributes     provider user-info 응답 원본
     */
    static OAuth2UserInfo of(String registrationId, Map<String, Object> attributes) {
        return switch (registrationId.toLowerCase()) {
            case "google" -> new GoogleUserInfo(attributes);
            case "kakao" -> new KakaoUserInfo(attributes);
            case "naver" -> new NaverUserInfo(attributes);
            default -> throw new IllegalArgumentException("지원하지 않는 OAuth provider: " + registrationId);
        };
    }
}
