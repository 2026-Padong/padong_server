package com.example.padong_server.domain.oauth.dto.response;

import com.example.padong_server.domain.oauth.entity.Role;
import com.example.padong_server.domain.oauth.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "ADMIN 전환 신청 응답")
public record AdminUpgradeResponse(
        @Schema(description = "유저 PK", example = "5") Long userId,
        @Schema(description = "전환 후 역할", example = "ADMIN") Role role,
        @Schema(description = "승인 여부 (신청 직후엔 false)", example = "false") boolean approved) {

    public static AdminUpgradeResponse from(User user) {
        return new AdminUpgradeResponse(user.getId(), user.getRole(), user.isApproved());
    }
}
