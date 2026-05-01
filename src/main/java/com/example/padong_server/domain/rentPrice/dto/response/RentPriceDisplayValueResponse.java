package com.example.padong_server.domain.rentPrice.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "매매/전세 대표 금액")
public record RentPriceDisplayValueResponse(
        @Schema(description = "대표 금액. 단위는 만원.", example = "120000", nullable = true)
        Long amount
) {
}
