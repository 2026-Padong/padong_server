package com.example.padong_server.domain.orderFlow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "모임 참여자 1명. 메뉴별 선택·수량·금액 + 결제 상태.")
public class OrderFlowParticipantResponse {

    @Schema(description = "사용자 ID", example = "42") private Long userId;
    @Schema(description = "사용자명", example = "최예일") private String userName;
    @Schema(description = "참여 시각") private LocalDateTime joinedAt;
    @Schema(description = "이 참여자가 고른 메뉴 항목들") private List<Item> items;
    @Schema(description = "주문 금액 합 (원)", example = "11000") private int totalAmount;
    @Schema(description = "결제 상태 (PENDING / PAID / CANCELED / REFUNDED 등)", example = "PAID")
    private String paymentStatus;

    @Getter
    @Builder
    @Schema(description = "참여자가 고른 메뉴 1줄")
    public static class Item {
        @Schema(description = "메뉴 ID", example = "7") private Long menuId;
        @Schema(description = "메뉴 이름 (스냅샷)", example = "통밀 식빵") private String menuInfo;
        @Schema(description = "단가 (원, 스냅샷)", example = "5500") private int price;
        @Schema(description = "수량", example = "2") private int quantity;
    }
}
