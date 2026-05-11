package com.example.padong_server.domain.recommendationLog.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RecommendationLogLikeRequest(
        @NotNull @Min(1) Long userId,
        @NotBlank String adminDongCode,
        @NotNull Boolean liked
) {}
