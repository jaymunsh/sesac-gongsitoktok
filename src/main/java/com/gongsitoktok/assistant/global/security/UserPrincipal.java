/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/global/security/UserPrincipal.java
 */
package com.gongsitoktok.assistant.global.security;

import com.gongsitoktok.assistant.user.entity.OauthService;

import java.io.Serializable;

/**
 * Spring Security {@code Authentication} 의 principal 로 들어가는 도메인 객체.
 *
 * <h3>설계 결정</h3>
 * <ul>
 *     <li>{@code UserDetails} 를 의도적으로 구현하지 않음 — 로컬·OAuth 양쪽 모두 JWT 토큰 검증으로 충분하고, 비밀번호·만료
 *         메타는 본 객체와 무관하다. 단순 데이터 캐리어로 유지해 컨트롤러 {@code @AuthenticationPrincipal} 주입을 깔끔하게 한다.</li>
 *     <li>{@link #userSeq} 는 불변 내부 PK — 모든 후속 도메인 조회의 기준 키.</li>
 *     <li>{@link #userId} 와 {@link #oauthService} 는 비즈니스 분기용 (예: 비밀번호 변경 endpoint 가 LOCAL 만 허용).</li>
 * </ul>
 *
 * @param userSeq      사용자 PK (JWT {@code sub} 와 동일)
 * @param userId       노출 ID (JWT custom claim {@code userId})
 * @param oauthService 가입 경로 (JWT custom claim {@code oauthService})
 */
public record UserPrincipal(Long userSeq, String userId, OauthService oauthService) implements Serializable {
}
