package com.example.padong_server.domain.recommendation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record AiDongneRecommendationResponse(
        @JsonProperty("user_type")
        String userType,
        List<Long> recommendations
) {}
