package com.example.padong_server.domain.rentPrice.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "거래유형 정보")
public record RentPriceTradeTypeResponse(
        @Schema(description = "거래유형 code", allowableValues = {"SALE", "JEONSE", "MONTHLY_RENT"}, example = "MONTHLY_RENT")
        String tradeTypeCode,

        @Schema(description = "거래유형 한글 라벨", allowableValues = {"매매", "전세", "월세"}, example = "월세")
        String tradeTypeLabel
) {
}
