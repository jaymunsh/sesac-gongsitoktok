/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/company/repository/CompanyRepository.java
 */
package com.gongsitoktok.assistant.company.repository;

import com.gongsitoktok.assistant.company.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 기업 리포지토리.
 *
 * <h3>nullable param 회피</h3>
 * <p>이전 버전은 단일 {@code @Query} 안에서 {@code (:corpCode IS NULL OR ...)} 분기로 동적 쿼리를 구성했으나,
 * PostgreSQL JDBC 가 {@code null} 파라미터를 {@code bytea} 로 추론해 {@code lower(bytea)} 함수를 찾다가
 * {@code 42883} 오류로 떨어지는 문제가 있었다. 동적 쿼리는 서비스 레이어에서 4 케이스 분기 호출로 처리하고,
 * 본 리포지토리는 단순 derived query 만 노출한다.</p>
 *
 * <p>외부 노출 키는 항상 {@code corpCode} 이며, FK 참조는 항상 {@code companySeq} 다.</p>
 */
public interface CompanyRepository extends JpaRepository<Company, Long> {

    /**
     * corpCode 단건 조회 (§4-7, §4-9, §4-14 upsert 매칭).
     */
    Optional<Company> findByCorpCode(String corpCode);

    /**
     * corpName 부분 일치(대소문자 무시) + 이름순 정렬 (§4-8).
     *
     * <p>Spring Data JPA 가 메서드 이름에서 SQL 을 생성하므로 빈 파라미터를 보낼 일이 없다.</p>
     */
    List<Company> findByCorpNameContainingIgnoreCaseOrderByCorpNameAsc(String corpName);
}
