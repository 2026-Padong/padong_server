package com.example.padong_server.domain.menu.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record MenuUpdateRequest(
        @Schema(description = "메뉴 설명", example = "모둠빵 세트")
        String menuInfo,
        @Schema(description = "정가", example = "4500")
        Integer originalPrice,
        @Schema(description = "할인가", example = "2500")
        Integer discountPrice,
        @Schema(description = "픽업 가능 시간", example = "11:00 ~ 16:00")
        String pickupAvailableTime,
        @Schema(description = "최대 모집 인원", example = "4")
        Integer maxParticipants,
        @Schema(description = "모집 마감 시간", example = "픽업 1시간 전")
        String recruitmentDeadline,
        @Schema(description = "결제 수단", example = "카드")
        String paymentMethod,
        @Schema(description = "현재 참여 인원", example = "2")
        Integer currentParticipants
) {
}
