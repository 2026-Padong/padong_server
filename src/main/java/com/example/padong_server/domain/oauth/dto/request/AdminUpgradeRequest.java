package com.example.padong_server.domain.oauth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "USER → ADMIN 전환 신청 (사업자등록증 파일은 multipart 의 businessLicense part 로)")
public record AdminUpgradeRequest(
        @Schema(description = "영업 행정동 PK (미입력 시 기존 거주 행정동 유지)", example = "80")
                Long adminDongId) {}
