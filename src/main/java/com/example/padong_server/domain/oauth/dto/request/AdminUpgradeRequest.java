package com.example.padong_server.domain.oauth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "USER → ADMIN 전환 신청")
public record AdminUpgradeRequest(
        @Schema(description = "사업자 등록증 이미지 URL", example = "https://s3.../license.png")
                @NotBlank
                String businessLicenseImageUrl,
        @Schema(description = "영업 행정동 PK (미입력 시 기존 거주 행정동 유지)", example = "80")
                Long adminDongId) {}
