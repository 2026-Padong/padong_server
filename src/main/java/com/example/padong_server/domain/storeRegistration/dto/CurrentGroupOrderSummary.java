package com.example.padong_server.domain.storeRegistration.dto;

import com.example.padong_server.domain.orderFlow.entity.OrderFlow;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "가게의 현재 활성 모임 요약 — 마감시간/최소주문 게이팅 용.")
public class CurrentGroupOrderSummary {

    @Schema(description = "OrderFlow PK", example = "42") private Long id;
    @Schema(description = "모집 마감 시각") private LocalDateTime recruitmentDeadline;
    @Schema(description = "1인당 최소 주문 수량", example = "1") private Integer minOrderPerPerson;

    public static CurrentGroupOrderSummary from(OrderFlow flow) {
        if (flow == null) return null;
        return CurrentGroupOrderSummary.builder()
                .id(flow.getId())
                .recruitmentDeadline(flow.getRecruitmentDeadline())
                .minOrderPerPerson(flow.getMinOrderPerPerson())
                .build();
    }
}
