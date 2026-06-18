/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/global/config/CorsConfig.java
 */
package com.gongsitoktok.assistant.global.config;

import com.gongsitoktok.assistant.global.filter.MdcTraceIdFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * CORS 설정 (제작요청 v6 §스텝 G).
 *
 * <h3>핵심 결정</h3>
 * <ul>
 *     <li><b>{@code allowCredentials=true}</b> — Refresh Token 이 httpOnly 쿠키로 발급되므로, 다른 도메인 프론트가
 *         {@code /api/v1/auth/refresh} 호출 시 쿠키를 함께 전송해야 한다.</li>
 *     <li><b>Origin 화이트리스트만</b> — {@code allowCredentials=true} 와 {@code Origin: *} 는 사양상 호환 불가.
 *         반드시 {@code app.cors.allowed-origins} 환경값(콤마 구분)에서 명시적 origin 만 허용.</li>
 *     <li><b>허용 메서드</b> — {@code GET, POST, PATCH, OPTIONS}. v6 endpoint 에 DELETE 가 없으므로 제외.</li>
 *     <li><b>{@code X-Trace-Id} 양방향 노출</b> — 요청 헤더 허용 + 응답 헤더 노출 (관측성).</li>
 * </ul>
 */
@Configuration
public class CorsConfig {

    @Value("${app.cors.allowed-origins}")
    private String allowedOriginsCsv;

    /**
     * 전역 CORS 구성. {@code SecurityConfig.cors(c -> c.configurationSource(...))} 에서 결선.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        List<String> origins = Arrays.stream(allowedOriginsCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();

        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(origins);
        cfg.setAllowedMethods(List.of(
                HttpMethod.GET.name(),
                HttpMethod.POST.name(),
                HttpMethod.PATCH.name(),
                HttpMethod.OPTIONS.name()
        ));
        cfg.setAllowedHeaders(List.of(
                HttpHeaders.AUTHORIZATION,
                HttpHeaders.CONTENT_TYPE,
                MdcTraceIdFilter.HEADER_TRACE_ID
        ));
        cfg.setExposedHeaders(List.of(
                MdcTraceIdFilter.HEADER_TRACE_ID
        ));
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", cfg);
        return src;
    }
}
