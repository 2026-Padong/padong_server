package com.example.padong_server.domain.storeRegistration.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record StoreRegistrationUpdateRequest(
        @Schema(description = "가게명", example = "파리바게뜨 연희안산점")
        String name,
        @Schema(description = "가게 주소", example = "서울 서대문구 연희로 11길 24")
        String address,
        @Schema(description = "가게 전화번호", example = "0507-2093-9485")
        String phoneNumber,
        @Schema(description = "운영시간", example = "10:00 ~ 15:00 (매일)")
        String operatingHours
) {
}
