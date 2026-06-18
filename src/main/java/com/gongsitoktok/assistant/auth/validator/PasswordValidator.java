/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/auth/validator/PasswordValidator.java
 */
package com.gongsitoktok.assistant.auth.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/**
 * {@link ValidPassword} 어노테이션의 실제 검증 로직.
 *
 * <h3>정책 (제작요청 v6 §4-1)</h3>
 * <ul>
 *     <li>최소 8자 이상</li>
 *     <li>대문자(A-Z) · 소문자(a-z) · 숫자(0-9) · 특수문자 각 1개 이상 포함</li>
 * </ul>
 *
 * <p>정규식 한 줄로 모두 검증한다. 백슬래시 이스케이프 주의 — Java 문자열에서 정규식 {@code \\d} 는 {@code \d}.</p>
 *
 * <h3>정적 헬퍼</h3>
 * <p>서비스 레이어가 직접 {@link #matchesPolicy(String)} 을 호출해 {@code PASSWORD_POLICY_VIOLATION} 코드로
 * BusinessException 을 던질 수 있도록 정적 진입점을 함께 제공한다.</p>
 */
public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {

    private static final Pattern POLICY = Pattern.compile(
            "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).{8,}$"
    );

    /**
     * 어노테이션 기반 검증 진입점.
     */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value != null && POLICY.matcher(value).matches();
    }

    /**
     * 서비스 레이어가 호출하는 명시 검증.
     *
     * @param raw 평문 비밀번호
     * @return 정책 만족 여부
     */
    public static boolean matchesPolicy(String raw) {
        return raw != null && POLICY.matcher(raw).matches();
    }
}
