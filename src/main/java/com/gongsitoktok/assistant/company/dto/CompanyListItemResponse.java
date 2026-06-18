/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/company/dto/CompanyListItemResponse.java
 */
package com.gongsitoktok.assistant.company.dto;

import com.gongsitoktok.assistant.company.entity.Company;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 기업 목록(메인페이지 그리드 · 검색 드롭다운) 항목 (제작요청 v6 §4-8).
 *
 * <p>표시에 필요한 최소 3필드만 노출 — 페이로드 절감.</p>
 */
@Schema(description = "기업 목록 항목")
public record CompanyListItemResponse(
        @Schema(description = "DART 기업 고유번호") String corpCode,
        @Schema(description = "기업명") String corpName,
        @Schema(description = "로고 URL (없으면 null)") String logoUrl
) {
    public static CompanyListItemResponse from(Company c) {
        return new CompanyListItemResponse(c.getCorpCode(), c.getCorpName(), c.getLogoUrl());
    }
}
