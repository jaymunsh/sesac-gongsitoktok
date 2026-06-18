/*
 * 상대 경로: src/main/java/com/gongsitoktok/assistant/user/dto/UserMeResponse.java
 */
package com.gongsitoktok.assistant.user.dto;

import com.gongsitoktok.assistant.user.entity.OauthService;
import com.gongsitoktok.assistant.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 마이페이지 조회 응답 (제작요청 v6 §4-4).
 *
 * <p>내부 식별자 {@code userSeq} 는 노출하지 않는다.</p>
 */
@Schema(description = "마이페이지 응답")
public record UserMeResponse(
        @Schema(description = "노출 ID", example = "alice01") String userId,
        @Schema(description = "표시명", example = "앨리스") String nickname,
        @Schema(description = "가입 경로", example = "LOCAL") OauthService oauthService,
        @Schema(description = "가입 시각") LocalDateTime createdAt
) {
    public static UserMeResponse from(User u) {
        return new UserMeResponse(u.getUserId(), u.getNickname(), u.getOauthService(), u.getCreatedAt());
    }
}
