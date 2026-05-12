package com.example.padong_server.domain.orderFlow.dto;

import com.example.padong_server.domain.menu.dto.MenuResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "모임(공구) 응답. 모집 정보 + 메뉴 묶음 + kitchen 상태 + 권한 플래그.")
public class OrderFlowResponse {

    @Schema(description = "OrderFlow PK", example = "42") private Long id;
    @Schema(description = "소속 가게 ID", example = "12") private Long storeId;
    /** legacy 메뉴 단건 호환. 신규 모임에선 menus[0] 으로 동일. */
    @Schema(description = "(legacy) 단일 메뉴 ID", example = "7") private Long menuId;
    @Schema(description = "(legacy) 단일 메뉴 이름", example = "통밀 식빵") private String menuInfo;
    @Schema(description = "모임에 묶인 메뉴 리스트 (sort_order 오름차순)") private List<MenuResponse> menus;
    @Schema(description = "모집 시작 시각") private LocalDateTime recruitmentStart;
    @Schema(description = "모집 마감 시각") private LocalDateTime recruitmentDeadline;
    @Schema(description = "1인당 최소 주문 수량", example = "1") private Integer minOrderPerPerson;
    @Schema(description = "결제 방식", example = "CARD") private String paymentMethod;
    @Schema(description = "현재 참여자 수", example = "3") private Integer participantCurrent;
    @Schema(description = "모집 정원", example = "10") private Integer participantTotal;
    @Schema(description = "상태 (PENDING / WAITING_APPROVAL / APPROVED / READY / COMPLETED / REJECTED)")
    private String status;
    @Schema(description = "마감 임박 여부 (PENDING 상태 + 마감 24h 이내)", example = "false")
    private boolean closingSoon;
    @Schema(description = "사장 측 취소 시각 (REJECTED + 모집중 취소 케이스에만 채워짐)")
    private LocalDateTime canceledAt;
    @Schema(description = "사장 측 취소 사유. REJECTED 라도 canceledAt 이 null 이면 일반 거절, 있으면 모임 취소.")
    private String canceledReason;
    @Schema(description = "사장 승인 가능 여부") private boolean canApprove;
    @Schema(description = "사장 거절 가능 여부") private boolean canReject;
    @Schema(description = "픽업 준비 완료 처리 가능 여부") private boolean canMarkReadyForPickup;
    @Schema(description = "픽업 완료 처리 가능 여부") private boolean canCompletePickup;
    @Schema(description = "모집중 취소 가능 여부") private boolean canCancel;
}
