/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/user/controller/UserController.java
 */
package com.gongsitoktok.assistant.user.controller;

import com.gongsitoktok.assistant.global.security.UserPrincipal;
import com.gongsitoktok.assistant.user.dto.PasswordChangeRequest;
import com.gongsitoktok.assistant.user.dto.UserMeResponse;
import com.gongsitoktok.assistant.user.dto.WithdrawResponse;
import com.gongsitoktok.assistant.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 사용자 컨트롤러 — 마이페이지 · 비밀번호 변경 · 탈퇴 (제작요청 v6 §4-4, §4-5, §4-6).
 */
@Tag(name = "User", description = "마이페이지 · 비밀번호 변경 · 탈퇴")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "마이페이지 조회")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "401", description = "INVALID_TOKEN")
    @ApiResponse(responseCode = "403", description = "USER_WITHDRAWN")
    @GetMapping("/me")
    public UserMeResponse me(@AuthenticationPrincipal UserPrincipal principal) {
        return userService.me(principal.userSeq());
    }

    @Operation(summary = "비밀번호 변경 (LOCAL 전용)",
            description = "현재 비밀번호 검증 → 새 비밀번호 정책 검증 → 다중 디바이스 일괄 무효화.")
    @ApiResponse(responseCode = "204", description = "변경 성공")
    @ApiResponse(responseCode = "400", description = "PASSWORD_POLICY_VIOLATION")
    @ApiResponse(responseCode = "401", description = "INVALID_TOKEN (현재 비밀번호 불일치)")
    @ApiResponse(responseCode = "403", description = "OAUTH_USER_PASSWORD_CHANGE_DENIED")
    @PatchMapping("/me/password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody PasswordChangeRequest req,
            HttpServletRequest request
    ) {
        String token = extractBearer(request);
        userService.changePassword(principal.userSeq(), req, token);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "회원 탈퇴 (Soft Delete)",
            description = "isActive=false + userId/providerId dismiss 변형. 이미 탈퇴된 경우 멱등 처리.")
    @ApiResponse(responseCode = "200", description = "탈퇴 처리 완료 (멱등 포함)")
    @PostMapping("/me/withdraw")
    public WithdrawResponse withdraw(
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest request
    ) {
        return userService.withdraw(principal.userSeq(), extractBearer(request));
    }

    private String extractBearer(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            return null;
        }
        return header.substring("Bearer ".length());
    }
}
