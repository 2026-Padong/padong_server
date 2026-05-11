package com.example.padong_server.domain.rentPrice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "행정동별 주거비 요약 조회 요청")
public record RentPriceSummaryRequest(
        @Schema(
                description = "조회할 행정동 코드 목록",
                example = "[\"1168064000\", \"1156054000\"]"
        )
        List<String> adminDongCodes,

        @Schema(
                description = "건물유형. 한글 라벨 또는 enum code(APARTMENT, OFFICETEL, ROW_MULTIFAMILY, DETACHED_MULTIFAMILY)를 사용할 수 있다. 미입력 시 단독다가구.",
                example = "아파트",
                nullable = true
        )
        String buildingTypeLabel,

        @Schema(
                description = "거래유형. 한글 라벨 또는 enum code(SALE, JEONSE, MONTHLY_RENT)를 사용할 수 있다. 미입력 시 월세.",
                example = "월세",
                nullable = true
        )
        String tradeTypeLabel
) {
}
