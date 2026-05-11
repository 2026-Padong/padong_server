package com.example.padong_server.domain.dongne.dto;

import com.example.padong_server.domain.activityMobility.dto.SafetyIndexResponse;
import com.example.padong_server.domain.path.dto.response.PathAllResponse;
import com.example.padong_server.domain.rentPrice.dto.response.AdminDongRentPriceDetailResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "동네 상세 응답")
public class DongneDetailResponse {

    @Schema(description = "출발 행정동(선택한 거주지 후보)")
    private DongneSummaryResponse departureDong;

    @Schema(description = "도착 행정동(직장 위치)")
    private DongneSummaryResponse arrivalDong;

    @Schema(description = "직장-거주지 기준 생활이동 정보")
    private DongneMobilityResponse mobility;

    @Schema(description = "총인구(명)", example = "41250.0")
    private Double totalPopulation;

    @Schema(description = "인구 밀도(명/km2)", example = "23150.4")
    private Double density;

    @Schema(description = "동네 안전지수 상세")
    private SafetyIndexResponse safety;

    @Schema(description = "전월세 상세 정보")
    private AdminDongRentPriceDetailResponse rentPrice;

    @Schema(description = "선택 동네 와 직장 행정동의 통합 길찾기(대중교통, 보행, 자동차). arrivalAdminDongCode 미입력이거나 출발=도착(같은 행정동)이면 null.")
    private PathAllResponse.Paths paths;

    @Schema(description = "좋아요 수", example = "23")
    private long likeCount;

    @Schema(description = "현재 사용자의 좋아요 여부", example = "true")
    private boolean likedByCurrentUser;
}
