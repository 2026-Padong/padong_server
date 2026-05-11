package com.example.padong_server.domain.recommendationLog.dto;

public record RecommendationLogUpdateResponse(
        Long userId,
        String adminDongCode,
        boolean updated
) {}
