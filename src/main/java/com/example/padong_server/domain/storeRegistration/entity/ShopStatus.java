package com.example.padong_server.domain.storeRegistration.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "가게 모집 상태")
public enum ShopStatus {
    @Schema(description = "모집 중") RECRUITING,
    @Schema(description = "마감 임박 (24h 이내)") CLOSING,
    @Schema(description = "마감") CLOSED
}
