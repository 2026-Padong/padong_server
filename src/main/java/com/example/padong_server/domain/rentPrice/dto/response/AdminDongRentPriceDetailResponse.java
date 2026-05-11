package com.example.padong_server.domain.rentPrice.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "행정동 주거 실거래가 상세 응답")
public record AdminDongRentPriceDetailResponse(
        @Schema(description = "행정동 코드", example = "1168064000")
        String adminDongCode,

        @Schema(description = "화면 표시용 통계 기간 라벨", example = "최근 2년 기준")
        String periodLabel,

        @Schema(description = "계약일 기준 시작일", example = "2024-04-18")
        String contractPeriodStart,

        @Schema(description = "계약일 기준 종료일", example = "2026-04-17")
        String contractPeriodEnd,

        @Schema(description = "취소된 매매거래 제외 여부", example = "true")
        boolean excludedCancelledSales,

        @Schema(description = "거래건수가 가장 많은 대표 건물유형. 거래가 없으면 null.")
        ResidenceBuildingTypeResponse dominantBuildingType,

        @Schema(description = "건물유형별 매매/전세/월세 대표 금액 목록")
        List<AdminDongRentPriceBuildingTypeResponse> buildingTypes
) {
}
