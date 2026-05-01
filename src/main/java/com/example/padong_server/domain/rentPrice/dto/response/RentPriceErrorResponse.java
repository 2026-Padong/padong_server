package com.example.padong_server.domain.rentPrice.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "rentPrice API 오류 응답")
public record RentPriceErrorResponse(
        @Schema(description = "오류 메시지", example = "존재하지 않는 행정동 코드입니다: 9999999999")
        String message
) {
}
