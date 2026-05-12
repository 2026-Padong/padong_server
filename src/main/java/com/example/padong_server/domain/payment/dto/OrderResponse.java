package com.example.padong_server.domain.payment.dto;

import com.example.padong_server.domain.payment.entity.Order;
import com.example.padong_server.domain.payment.entity.OrderMenu;
import com.example.padong_server.domain.payment.entity.Payment;
import com.example.padong_server.domain.storeRegistration.entity.Store;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.Instant;
import java.util.List;

@Schema(description = "주문 상세")
public record OrderResponse(
        @Schema(description = "주문 PK (= cursor 값)", example = "42") Long orderId,
        @Schema(description = "외부 노출용 주문번호", example = "20260512-00042") String orderNumber,
        @Schema(description = "PortOne paymentId (UUID)") String paymentId,
        @Schema(description = "결제 완료 시각 (UTC). 미결제 상태면 null", example = "2026-05-12T01:23:45Z")
                Instant paidAt,
        @Schema(description = "결제 수단 (PortOne 결제수단, 미결제 시 null)", example = "card")
                String paymentMethod,
        @Schema(description = "결제 금액(원)", example = "12000") Long totalAmount,
        @Schema(description = "주문 상태 (READY | PAID | CANCELED | FAILED)", example = "PAID")
                String status,
        @Schema(
                description =
                        "fulfillment 상태 (모임 OrderFlow 기준). PENDING/WAITING_APPROVAL/APPROVED/READY/COMPLETED/REJECTED. 매칭 OrderFlow 없으면 null.",
                example = "APPROVED")
                String flowStatus,
        @Schema(description = "가게 정보") OrderShopDto shop,
        @Schema(description = "주문 메뉴 목록") List<OrderItemDto> items) {

    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");

    @Schema(description = "주문 가게 요약")
    public record OrderShopDto(
            Long id,
            String name,
            String imageUrl,
            String category,
            String address,
            String phoneNumber,
            String openTime,
            String closeTime) {

        public static OrderShopDto from(Store s) {
            return new OrderShopDto(
                    s.getId(),
                    s.getName(),
                    s.getThumbnailUrl(),
                    s.getCategory() == null ? null : s.getCategory().name(),
                    s.getAddress(),
                    s.getPhoneNumber(),
                    formatTime(s.getOpenTime()),
                    formatTime(s.getCloseTime()));
        }

        private static String formatTime(LocalTime t) {
            return t == null ? null : t.format(HHMM);
        }
    }

    @Schema(description = "주문 메뉴 라인")
    public record OrderItemDto(Long menuId, String name, Long price, Integer quantity) {

        public static OrderItemDto from(OrderMenu om) {
            return new OrderItemDto(
                    om.getMenu().getId(),
                    om.getMenu().getMenuInfo(),
                    (long) om.getPrice(),
                    om.getQuantity());
        }
    }

    public static OrderResponse from(
            Order order, Payment payment, List<OrderMenu> items, String flowStatus) {
        Instant paidAtInstant = null;
        if (payment != null && payment.getPaidAt() != null) {
            paidAtInstant = payment.getPaidAt().atZone(ZoneId.systemDefault()).toInstant();
        }
        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                payment == null ? null : payment.getPaymentId(),
                paidAtInstant,
                payment == null ? null : payment.getPaymentMethod(),
                payment == null ? (long) order.getTotalPrice() : payment.getTotalAmount(),
                order.getOrderStatus().name(),
                flowStatus,
                OrderShopDto.from(order.getStore()),
                items.stream().map(OrderItemDto::from).toList());
    }

    // 사용 X — service 에서 LocalDateTime 직접 받을 경우 대비
    public static Instant toInstant(LocalDateTime dt) {
        return dt == null ? null : dt.atZone(ZoneId.systemDefault()).toInstant();
    }
}
