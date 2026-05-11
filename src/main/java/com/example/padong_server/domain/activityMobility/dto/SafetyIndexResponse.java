package com.example.padong_server.domain.activityMobility.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "동네 안전지수 상세 응답")
public record SafetyIndexResponse(
        @Schema(description = "종합 안전지수 등급", example = "C")
        String overallScore,

        @Schema(description = "생활안전 등급", example = "C")
        String lifeSafetyGrade,

        @Schema(description = "교통사고 등급", example = "C")
        String trafficAccidentGrade,

        @Schema(description = "화재 등급", example = "D")
        String fireGrade,

        @Schema(description = "범죄 등급", example = "E")
        String crimeGrade
) {
}
