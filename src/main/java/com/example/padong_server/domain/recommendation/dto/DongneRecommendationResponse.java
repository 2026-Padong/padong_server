package com.example.padong_server.domain.recommendation.dto;

import com.example.padong_server.domain.activityMobility.dto.MobilitySimpleResponse;
import com.example.padong_server.global.PageResponse;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 출퇴근(/mobility) 응답과 카드 shape 일관성을 위해 {@code page} 안에
 * {@link MobilitySimpleResponse} 페이지를 그대로 노출. 프론트는 같은 매퍼 재사용.
 */
public record DongneRecommendationResponse(
        @Schema(description = "AI 분류 사용자 유형 라벨", example = "집돌이") String userType,
        @Schema(description = "추천 동네 카드 페이지")
                PageResponse<MobilitySimpleResponse> page) {}
