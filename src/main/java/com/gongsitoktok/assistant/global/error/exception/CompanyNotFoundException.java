/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/global/error/exception/CompanyNotFoundException.java
 */
package com.gongsitoktok.assistant.global.error.exception;

import com.gongsitoktok.assistant.global.error.ErrorCode;

/**
 * 요청된 {@code corpCode} 의 기업이 {@code tb_company} 에 없을 때 발생.
 *
 * <p>운영자가 §4-14 PATCH 로 등록하기 전까지는 어떤 기업이라도 본 예외로 떨어진다.</p>
 */
public class CompanyNotFoundException extends BusinessException {

    public CompanyNotFoundException(String corpCode) {
        super(ErrorCode.COMPANY_NOT_FOUND, "기업을 찾을 수 없습니다: " + corpCode);
    }
}
