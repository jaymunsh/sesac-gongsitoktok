/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/company/service/CompanyService.java
 */
package com.gongsitoktok.assistant.company.service;

import com.gongsitoktok.assistant.company.dto.CompanyListItemResponse;
import com.gongsitoktok.assistant.company.dto.CompanyResponse;
import com.gongsitoktok.assistant.company.entity.Company;
import com.gongsitoktok.assistant.company.repository.CompanyRepository;
import com.gongsitoktok.assistant.global.error.exception.CompanyNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * 기업 조회 서비스 (제작요청 v6 §4-7, §4-8).
 *
 * <h3>{@link #search(String, String)} 분기</h3>
 * <ul>
 *     <li>둘 다 있음 — corpCode 단건 조회 + corpName 부분 일치 필터 (AND)</li>
 *     <li>corpCode 만 — 단건 조회</li>
 *     <li>corpName 만 — Containing IgnoreCase 정렬 조회</li>
 *     <li>둘 다 없음 — 전체 조회 (corpName 오름차순)</li>
 * </ul>
 *
 * <p>JPQL 안에 {@code (:param IS NULL OR ...)} 같은 nullable 분기를 두지 않는다 — PostgreSQL JDBC 가 null 파라미터를
 * {@code bytea} 로 추론해 발생하는 {@code lower(bytea) does not exist} 오류 회피.</p>
 */
@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;

    /**
     * 단건 조회 — 미존재 시 {@link CompanyNotFoundException}.
     */
    @Transactional(readOnly = true)
    public CompanyResponse getByCorpCode(String corpCode) {
        Company c = companyRepository.findByCorpCode(corpCode)
                .orElseThrow(() -> new CompanyNotFoundException(corpCode));
        return CompanyResponse.from(c);
    }

    /**
     * 4 케이스 분기 검색.
     */
    @Transactional(readOnly = true)
    public List<CompanyListItemResponse> search(String corpCode, String corpName) {
        String code = normalize(corpCode);
        String name = normalize(corpName);

        if (code != null && name != null) {
            String needle = name.toLowerCase(Locale.ROOT);
            return companyRepository.findByCorpCode(code)
                    .filter(c -> c.getCorpName().toLowerCase(Locale.ROOT).contains(needle))
                    .map(c -> List.of(CompanyListItemResponse.from(c)))
                    .orElseGet(List::of);
        }
        if (code != null) {
            return companyRepository.findByCorpCode(code)
                    .map(c -> List.of(CompanyListItemResponse.from(c)))
                    .orElseGet(List::of);
        }
        if (name != null) {
            return companyRepository.findByCorpNameContainingIgnoreCaseOrderByCorpNameAsc(name)
                    .stream().map(CompanyListItemResponse::from).toList();
        }
        return companyRepository.findAll(Sort.by(Sort.Direction.ASC, "corpName"))
                .stream().map(CompanyListItemResponse::from).toList();
    }

    private String normalize(String v) {
        if (v == null) {
            return null;
        }
        String trimmed = v.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
