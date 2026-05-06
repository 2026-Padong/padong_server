package com.example.padong_server.domain.dongne.dto;

import com.example.padong_server.domain.rentPrice.dto.response.AdminDongRentPriceDetailResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "동네 상세 응답")
public class DongneDetailResponse {

    @Schema(description = "선택된 동네 정보")
    private DongneSummaryResponse dongne;

    @Schema(description = "직장 위치 기준 동네 정보")
    private DongneSummaryResponse workDong;

    @Schema(description = "직장-거주지 기준 생활이동 정보")
    private DongneMobilityResponse mobility;

    @Schema(description = "총 인구", example = "12340.0")
    private Double totalPopulation;

    @Schema(description = "인구 밀도", example = "8920.0")
    private Double density;

    @Schema(description = "임대료 상세 정보")
    private AdminDongRentPriceDetailResponse rentPrice;

    @Schema(description = "좋아요 수", example = "12")
    private long likeCount;

    @Schema(description = "현재 사용자의 좋아요 여부", example = "true")
    private boolean likedByCurrentUser;
}
