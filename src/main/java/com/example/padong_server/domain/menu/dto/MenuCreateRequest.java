package com.example.padong_server.domain.menu.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record MenuCreateRequest(
        @Schema(description = "가게 ID", example = "1")
        Long storeId,
        @Schema(description = "메뉴 설명", example = "모둠빵 세트")
        String menuInfo,
        @Schema(description = "정가", example = "5000")
        Integer originalPrice,
        @Schema(description = "할인가", example = "3000")
        Integer discountPrice,
        @Schema(description = "픽업 가능 시간", example = "10:00 ~ 15:00")
        String pickupAvailableTime,
        @Schema(description = "최대 모집 인원", example = "5")
        Integer maxParticipants,
        @Schema(description = "모집 마감 시간", example = "픽업 30분 전")
        String recruitmentDeadline,
        @Schema(description = "결제 수단", example = "카드 / 간편결제")
        String paymentMethod,
        @Schema(description = "현재 참여 인원", example = "1")
        Integer currentParticipants
) {
}
