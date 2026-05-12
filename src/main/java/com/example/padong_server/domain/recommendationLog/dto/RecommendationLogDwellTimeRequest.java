package com.example.padong_server.domain.recommendationLog.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RecommendationLogDwellTimeRequest(
        @NotNull @Min(1) Long userId,
        @NotBlank String adminDongCode,
        @NotNull @Min(0) Integer dwellTimeSec
) {}
