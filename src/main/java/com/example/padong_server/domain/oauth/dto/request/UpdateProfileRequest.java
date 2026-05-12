package com.example.padong_server.domain.oauth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "프로필 수정 요청 (JSON part)")
public record UpdateProfileRequest(
        @Schema(description = "변경할 닉네임", example = "지윤")
        @NotBlank
        @Size(min = 2, max = 20)
        String nickname) {}
