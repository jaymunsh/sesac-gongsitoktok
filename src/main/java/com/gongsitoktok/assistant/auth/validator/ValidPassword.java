/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/auth/validator/ValidPassword.java
 */
package com.gongsitoktok.assistant.auth.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 비밀번호 정책 검증 어노테이션.
 *
 * <p>제작요청 v6 §4-1 의 정책을 그대로 위임 검증 — 8자 이상 + 대문자/소문자/숫자/특수문자 각 1개 이상.</p>
 *
 * <p>본 어노테이션이 직접 실패하면 {@code MethodArgumentNotValidException} → {@code GlobalExceptionHandler} 가
 * 일반적인 {@code VALIDATION_FAILED} 코드로 응답한다. 그러나 §4-1 사양은 비밀번호에 한해 별도 코드
 * {@code PASSWORD_POLICY_VIOLATION} 을 요구하므로, 서비스 진입 시 {@link PasswordValidator#matchesPolicy(String)}
 * 정적 메서드를 호출해 명시적으로 분기·throw 하는 패턴을 함께 사용한다.</p>
 *
 * @see PasswordValidator
 */
@Documented
@Constraint(validatedBy = PasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPassword {

    String message() default "비밀번호 정책(8자 이상, 대/소문자·숫자·특수문자 각 1개 이상)을 만족해야 합니다.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
