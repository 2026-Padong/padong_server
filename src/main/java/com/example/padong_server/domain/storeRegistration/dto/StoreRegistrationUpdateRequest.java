package com.example.padong_server.domain.storeRegistration.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalTime;

public record StoreRegistrationUpdateRequest(
        @Schema(description = "가게명", example = "파동 식당")
        String name,
        @Schema(description = "가게 주소", example = "서울 송파구 올림픽로 300")
        String address,
        @Schema(description = "가게 전화번호", example = "0507-2093-9485")
        String phoneNumber,
        @Schema(description = "오픈 시간", example = "10:00")
        LocalTime openTime,
        @Schema(description = "마감 시간", example = "15:00")
        LocalTime closeTime
) {
}
