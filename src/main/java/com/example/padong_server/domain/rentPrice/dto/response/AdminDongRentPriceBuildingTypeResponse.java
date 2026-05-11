package com.example.padong_server.domain.rentPrice.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "건물유형별 주거 실거래가 대표 금액")
public record AdminDongRentPriceBuildingTypeResponse(
        @Schema(description = "건물유형")
        ResidenceBuildingTypeResponse buildingType,

        @Schema(description = "매매 대표 금액. 단위는 만원.")
        RentPriceDisplayValueResponse sale,

        @Schema(description = "전세 대표 보증금. 단위는 만원.")
        RentPriceDisplayValueResponse jeonse,

        @Schema(description = "월세 대표 보증금/월세금. 단위는 만원.")
        MonthlyRentDisplayValueResponse monthlyRent
) {
}
