package com.example.padong_server.domain.rentPrice.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "행정동 주거비 요약 응답")
public record AdminDongRentPriceSummaryResponse(
        @Schema(description = "행정동 코드", example = "1168064000")
        String adminDongCode,

        @Schema(description = "화면 표시용 통계 기간 라벨", example = "최근 2년 기준")
        String periodLabel,

        @Schema(description = "요청 또는 기본값으로 선택된 건물유형")
        ResidenceBuildingTypeResponse buildingType,

        @Schema(description = "요청 또는 기본값으로 선택된 거래유형")
        RentPriceTradeTypeResponse tradeType,

        @Schema(description = "매매 대표 금액. 단위는 만원. 선택 거래유형이 매매가 아니면 amount는 null.")
        RentPriceDisplayValueResponse sale,

        @Schema(description = "전세 대표 보증금. 단위는 만원. 선택 거래유형이 전세가 아니면 amount는 null.")
        RentPriceDisplayValueResponse jeonse,

        @Schema(description = "월세 대표 보증금/월세금. 단위는 만원. 선택 거래유형이 월세가 아니면 값은 null.")
        MonthlyRentDisplayValueResponse monthlyRent
) {
}
