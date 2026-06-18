/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/global/config/SecurityConfig.java
 */
package com.gongsitoktok.assistant.global.config;

import com.gongsitoktok.assistant.global.filter.MdcTraceIdFilter;
import com.gongsitoktok.assistant.global.security.JsonAuthenticationEntryPoint;
import com.gongsitoktok.assistant.global.security.JwtAuthenticationFilter;
import com.gongsitoktok.assistant.global.security.RestAccessDeniedHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Spring Security 설정 (제작요청 v6 §스텝 H-1, §스텝 G).
 *
 * <h3>필터 체인 (논리적 진입 순서)</h3>
 * <ol>
 *     <li>{@link MdcTraceIdFilter} — 모든 요청에 traceId 바인딩 (서블릿 레벨, Security 체인 외부에서 동작).</li>
 *     <li>{@link JwtAuthenticationFilter} — {@code UsernamePasswordAuthenticationFilter} 자리에 결선.</li>
 *     <li>OAuth2 Login — Phase 4 에서 {@link AuthenticationSuccessHandler} / {@link AuthenticationFailureHandler}
 *         빈이 등록되면 본 설정이 자동으로 결선한다 ({@link ObjectProvider} 패턴).</li>
 * </ol>
 *
 * <h3>인가 규칙</h3>
 * <ul>
 *     <li>{@code permitAll}: signup, login, refresh, OAuth2 진입·콜백, 공개 기업 조회, Swagger, health,
 *         <b>{@code /api/v1/internal/**} (v6 임시)</b>.</li>
 *     <li>{@code authenticated}: logout, users/me, chat.</li>
 *     <li>그 외 {@code denyAll}.</li>
 * </ul>
 *
 * <h3>STATELESS 세션</h3>
 * <p>JWT 헤더 인증이므로 서버 측 세션 사용 금지. CSRF 도 disable (REST + JWT 헤더 구조).</p>
 *
 * <h3>⚠️ {@code /api/v1/internal/**}</h3>
 * <p>v6 한정 permitAll. 운영 배포 전 인프라 레벨(LB IP 화이트리스트 / 사내망 바인딩 / endpoint 자체 비활성) 보호 필수.
 * 자세한 옵션은 {@code archive/later.md #1}.</p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_PATHS = {
            "/api/v1/auth/signup",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/oauth2/**",
            "/login/oauth2/**",
            "/api/v1/companies/**",
            "/api/v1/internal/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/actuator/health"
    };

    private static final String[] AUTHENTICATED_PATHS = {
            "/api/v1/auth/logout",
            "/api/v1/users/me/**",
            "/api/v1/chat/**"
    };

    /**
     * MDC 필터를 모든 요청에 가장 먼저 적용하기 위해 별도 등록.
     *
     * <p>{@code Ordered.HIGHEST_PRECEDENCE} — OAuth2 콜백·정적 리소스·에러 페이지까지 빠짐없이 traceId 가 붙는다.</p>
     */
    @Bean
    public FilterRegistrationBean<MdcTraceIdFilter> mdcTraceIdFilterRegistration(MdcTraceIdFilter filter) {
        FilterRegistrationBean<MdcTraceIdFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE);
        reg.addUrlPatterns("/*");
        return reg;
    }

    /**
     * SecurityFilterChain 본체.
     *
     * @param http                    HttpSecurity DSL
     * @param corsConfigurationSource {@link CorsConfig#corsConfigurationSource()}
     * @param jwtAuthenticationFilter JWT 필터
     * @param oauth2UserService       {@link OAuth2UserService} — Phase 4 에서 {@code CustomOAuth2UserService} 빈으로 대체됨.
     *                                미등록 시 {@link DefaultOAuth2UserService} 로 폴백 (OAuth2 자체는 비활성과 다름 — 인증은 되지만
     *                                우리 도메인 매핑이 안 됨).
     * @param successHandler          {@link AuthenticationSuccessHandler} — Phase 4 에서 등록. 미등록 시 OAuth2 콜백이 기본 동작.
     * @param failureHandler          {@link AuthenticationFailureHandler} — Phase 4 에서 등록.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            CorsConfigurationSource corsConfigurationSource,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            JsonAuthenticationEntryPoint jsonAuthenticationEntryPoint,
            RestAccessDeniedHandler restAccessDeniedHandler,
            ObjectProvider<OAuth2UserService<OAuth2UserRequest, OAuth2User>> oauth2UserService,
            ObjectProvider<AuthenticationSuccessHandler> successHandler,
            ObjectProvider<AuthenticationFailureHandler> failureHandler
    ) throws Exception {

        OAuth2UserService<OAuth2UserRequest, OAuth2User> userSvc = oauth2UserService
                .getIfAvailable(DefaultOAuth2UserService::new);

        http
                .csrf(c -> c.disable())
                .formLogin(c -> c.disable())
                .httpBasic(c -> c.disable())
                .cors(c -> c.configurationSource(corsConfigurationSource))
                .sessionManagement(c -> c.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // REST + JWT 구조 — 401/403 모두 JSON 으로 응답 (302 redirect 차단)
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(jsonAuthenticationEntryPoint)
                        .accessDeniedHandler(restAccessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .requestMatchers(AUTHENTICATED_PATHS).authenticated()
                        .anyRequest().denyAll()
                )
                .oauth2Login(oauth -> {
                    oauth.userInfoEndpoint(u -> u.userService(userSvc));
                    successHandler.ifAvailable(oauth::successHandler);
                    failureHandler.ifAvailable(oauth::failureHandler);
                })
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * AuthenticationManager — 로컬 로그인은 {@code AuthService} 가 직접 검증하지만, Spring Security DSL 일관성을 위해 노출.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
