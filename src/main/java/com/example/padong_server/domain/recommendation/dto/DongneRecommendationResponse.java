package com.example.padong_server.domain.recommendation.dto;

import com.example.padong_server.domain.dongne.dto.DongneSummaryResponse;
import java.util.List;

public record DongneRecommendationResponse(
        String userType,
        List<Long> recommendationIds,
        List<DongneSummaryResponse> recommendations
) {}
