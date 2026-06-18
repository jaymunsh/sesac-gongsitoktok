/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/user/entity/OauthService.java
 */
package com.gongsitoktok.assistant.user.entity;

/**
 * 사용자의 가입 경로를 구분하는 enum.
 *
 * <p>본 enum 값은 다음 두 곳에서 핵심적으로 사용된다.</p>
 * <ul>
 *     <li>{@code tb_user.oauth_service} 컬럼 — {@code @Enumerated(EnumType.STRING)} 매핑.</li>
 *     <li>JWT custom claim {@code oauthService} — 인가 분기(예: 비밀번호 변경은 LOCAL 만 허용)에 사용.</li>
 * </ul>
 *
 * <h3>(oauthService, providerId) 복합 유니크 제약</h3>
 * <ul>
 *     <li>{@link #LOCAL} 사용자는 {@code providerId} 가 NULL 이며, PostgreSQL 의 "NULL 다중 허용" 규칙에 의해 중복 검사에서 자연 제외된다.</li>
 *     <li>{@link #GOOGLE}, {@link #KAKAO}, {@link #NAVER} 는 항상 provider 가 발급한 {@code providerId} 를 갖는다.
 *         탈퇴 시에는 {@code providerId} 끝에 {@code #dismiss_{epochMillis}} 가 붙어 동일 OAuth 계정으로의 재가입을 허용한다.</li>
 * </ul>
 */
public enum OauthService {

    /** 로컬(아이디·비밀번호) 가입. {@code providerId} 는 NULL, {@code password} 는 BCrypt 해시 보관. */
    LOCAL,

    /** Google OAuth2 / OIDC. {@code providerId} 는 ID Token 의 {@code sub} 클레임. */
    GOOGLE,

    /** Kakao OAuth2. {@code providerId} 는 카카오 user-info 응답의 {@code id}. */
    KAKAO,

    /** Naver OAuth2. {@code providerId} 는 네이버 user-info 응답의 {@code response.id}. */
    NAVER
}
