package com.example.padong_server.domain.recommendationLog.dto;

import java.time.LocalDateTime;

public record RecommendationLogRow(
        Long id,
        Long userId,
        String adminDongCode,
        Integer rankPosition,
        Boolean impression,
        Integer clickedCount,
        Integer likedCount,
        Integer dwellTimeSec,
        RecommendationType recommendationType,
        Integer q1,
        Integer q2,
        Integer q3,
        Integer q4,
        Integer q5,
        Integer q6,
        Integer q7,
        Integer q8,
        Integer q9,
        Integer q10,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
