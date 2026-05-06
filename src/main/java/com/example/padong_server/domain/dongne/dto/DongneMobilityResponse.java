package com.example.padong_server.domain.dongne.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "생활이동 요약 정보")
public record DongneMobilityResponse(
        @Schema(description = "최근 대표 이동량", example = "8920.12")
        Double totalMobility,

        @Schema(description = "평균 이동 시간(분)", example = "35.20")
        Double avgTime,

        @Schema(description = "집계 시작월", example = "202601")
        String startMonth,

        @Schema(description = "집계 종료월", example = "202603")
        String endMonth
) {
    public static DongneMobilityResponse empty() {
        return new DongneMobilityResponse(null, null, null, null);
    }
}
