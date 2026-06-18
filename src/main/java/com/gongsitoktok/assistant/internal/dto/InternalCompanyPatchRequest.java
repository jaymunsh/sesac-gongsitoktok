/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/internal/dto/InternalCompanyPatchRequest.java
 */
package com.gongsitoktok.assistant.internal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * 운영자용 기업 upsert 요청 (제작요청 v6 §4-14).
 *
 * <ul>
 *     <li>기존 행 갱신 — 모든 필드 optional.</li>
 *     <li>신규 생성 — {@link #corpName} 필수 (서비스에서 명시 검증, 누락 시 {@code VALIDATION_FAILED}).</li>
 * </ul>
 */
@Schema(description = "운영자 기업 upsert 요청")
public record InternalCompanyPatchRequest(
        @Schema(description = "기업명") @Size(max = 255) String corpName,
        @Schema(description = "로고 URL (CDN)") @Size(max = 500) String logoUrl,
        @Schema(description = "기업 요약본") String summaryContent
) {
}
