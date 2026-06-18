/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/company/entity/Company.java
 */
package com.gongsitoktok.assistant.company.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 기업 요약 엔티티 — {@code tb_company}.
 *
 * <h3>관리 주체</h3>
 * <p>본 테이블은 운영자가 §4-14 PATCH endpoint 로 등록·갱신한다. FastAPI 는 본 테이블을 자동으로 채우지 않는다
 * (FastAPI ↔ Spring Boot 통신은 §3-1 챗봇 응답 한 방향만 존재).</p>
 *
 * <h3>식별자 이중 구조</h3>
 * <ul>
 *     <li>{@link #companySeq} — 내부 불변 PK. {@code tb_chat_room} 의 FK 참조 대상. 외부 노출 금지.</li>
 *     <li>{@link #corpCode}   — DART 공식 기업 고유번호. 외부 노출 비즈니스 키 + 운영자 upsert 매칭 키.</li>
 * </ul>
 */
@Entity
@Table(
        name = "tb_company",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_company_corp_code", columnNames = "corp_code")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "company_seq")
    private Long companySeq;

    /** DART 공식 기업 고유번호. 외부 노출 + upsert 매칭 키. */
    @Column(name = "corp_code", nullable = false)
    private String corpCode;

    /** 기업명. 운영자가 등록·수정. */
    @Column(name = "corp_name", nullable = false)
    private String corpName;

    /**
     * 외부 호스팅(CDN 등) 로고 이미지 URL. 바이너리는 DB 에 보관하지 않음.
     * 미등록 시 NULL — 프론트에서 placeholder 이미지로 대체.
     */
    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    /** 기업 단위 요약본. 운영자가 §4-14 endpoint 로 작성. */
    @Column(name = "summary_content", columnDefinition = "TEXT")
    private String summaryContent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 정적 팩토리 — 신규 생성.
     *
     * @param corpCode       DART 기업 고유번호 (unique)
     * @param corpName       기업명 (NOT NULL)
     * @param logoUrl        로고 URL (nullable)
     * @param summaryContent 요약본 (nullable)
     */
    public static Company create(String corpCode, String corpName, String logoUrl, String summaryContent) {
        Company c = new Company();
        c.corpCode = corpCode;
        c.corpName = corpName;
        c.logoUrl = logoUrl;
        c.summaryContent = summaryContent;
        return c;
    }

    /**
     * 부분 갱신 (true partial update). 인자로 null 이 들어오면 해당 필드는 변경하지 않는다.
     *
     * <p>{@code corpCode} 는 비즈니스 키로 변경 불가하므로 본 메서드에서 다루지 않는다.</p>
     */
    public void patch(String corpName, String logoUrl, String summaryContent) {
        if (corpName != null) {
            this.corpName = corpName;
        }
        if (logoUrl != null) {
            this.logoUrl = logoUrl;
        }
        if (summaryContent != null) {
            this.summaryContent = summaryContent;
        }
    }
}
