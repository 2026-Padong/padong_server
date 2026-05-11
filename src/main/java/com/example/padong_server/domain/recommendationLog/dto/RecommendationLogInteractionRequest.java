package com.example.padong_server.domain.recommendationLog.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RecommendationLogInteractionRequest(
        @NotNull @Min(1) Long userId,
        @NotBlank String adminDongCode,
        Boolean clicked,
        Boolean liked,
        @Min(0) Integer dwellTimeSec
) {}
