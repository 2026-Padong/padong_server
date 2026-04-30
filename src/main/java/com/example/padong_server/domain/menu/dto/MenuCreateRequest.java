package com.example.padong_server.domain.menu.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record MenuCreateRequest(
        @Schema(description = "Store ID", example = "1")
        Long storeId,
        @Schema(description = "Menu description", example = "Assorted bread set")
        String menuInfo,
        @Schema(description = "Original price", example = "5000")
        Integer originalPrice,
        @Schema(description = "Discount price", example = "3000")
        Integer discountPrice,
        @Schema(description = "Pickup available time", example = "10:00 ~ 15:00")
        String pickupAvailableTime,
        @Schema(description = "Maximum participants", example = "5")
        Integer maxParticipants,
        @Schema(description = "Recruitment deadline", example = "30 minutes before pickup")
        String recruitmentDeadline,
        @Schema(description = "Payment method", example = "Card / Easy payment")
        String paymentMethod,
        @Schema(description = "Current participants", example = "1")
        Integer currentParticipants
) {
}
