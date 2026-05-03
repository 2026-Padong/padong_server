package com.example.padong_server.domain.menu.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record MenuUpdateRequest(
        @Schema(description = "Menu description", example = "Assorted bread set")
        String menuInfo,
        @Schema(description = "Original price", example = "4500")
        Integer originalPrice,
        @Schema(description = "Discount price", example = "2500")
        Integer discountPrice,
        @Schema(description = "Pickup available time", example = "11:00 ~ 16:00")
        String pickupAvailableTime,
        @Schema(description = "Maximum participants", example = "4")
        Integer maxParticipants,
        @Schema(description = "Recruitment deadline", example = "1 hour before pickup")
        String recruitmentDeadline,
        @Schema(description = "Payment method", example = "Card")
        String paymentMethod,
        @Schema(description = "Current participants", example = "2")
        Integer currentParticipants
) {
}
