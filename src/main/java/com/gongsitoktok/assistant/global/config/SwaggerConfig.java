/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/global/config/SwaggerConfig.java
 */
package com.gongsitoktok.assistant.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Springdoc OpenAPI v2 설정 (제작요청 v6 §핵심 개발 제약 #2 · §4-14 운영자 endpoint 그룹 분리).
 *
 * <h3>그룹 분리</h3>
 * <ul>
 *     <li>{@code public}   — 프론트엔드가 호출하는 모든 정식 API ({@code /api/v1/auth/**}, {@code /users/**}, {@code /companies/**}, {@code /chat/**}).</li>
 *     <li>{@code internal} — 운영자 전용 ({@code /api/v1/internal/**}). 노출 의도 자체를 가리기 위해 별도 그룹.</li>
 * </ul>
 *
 * <h3>인증 스킴</h3>
 * <p>Bearer JWT 시큐리티 스킴을 정의해 Swagger UI 에서 "Authorize" 버튼으로 Access Token 을 주입할 수 있다.</p>
 */
@Configuration
public class SwaggerConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI gongsitoktokOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("공시톡톡 어시스턴트 API")
                        .description("공시 분석 어시스턴트 백엔드 — Spring Boot ↔ FastAPI 동기 JSON 통신 (v2.0)")
                        .version("v6"))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }

    @Bean
    public GroupedOpenApi publicApiGroup() {
        return GroupedOpenApi.builder()
                .group("public")
                .pathsToMatch(
                        "/api/v1/auth/**",
                        "/api/v1/users/**",
                        "/api/v1/companies/**",
                        "/api/v1/chat/**"
                )
                .build();
    }

    @Bean
    public GroupedOpenApi internalApiGroup() {
        return GroupedOpenApi.builder()
                .group("internal")
                .pathsToMatch("/api/v1/internal/**")
                .build();
    }
}
