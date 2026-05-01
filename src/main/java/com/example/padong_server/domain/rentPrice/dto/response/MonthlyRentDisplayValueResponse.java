package com.example.padong_server.domain.rentPrice.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "월세 대표 금액")
public record MonthlyRentDisplayValueResponse(
        @Schema(description = "대표 보증금. 단위는 만원.", example = "5000", nullable = true)
        Long deposit,

        @Schema(description = "대표 월세금. 단위는 만원.", example = "180", nullable = true)
        Long monthlyRent
) {
}
