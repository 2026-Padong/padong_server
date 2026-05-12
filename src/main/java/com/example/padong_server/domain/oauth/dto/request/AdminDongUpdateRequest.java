package com.example.padong_server.domain.oauth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "거주 행정동 변경 요청")
public record AdminDongUpdateRequest(
        @Schema(description = "변경할 AdminDong PK", example = "80") @NotNull Long adminDongId) {}
