package com.example.padong_server.domain.orderFlow.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "모임(공구) 생성 요청")
public record OrderFlowCreateRequest(
        @Schema(description = "가게 ID", example = "12") @NotNull Long storeId,
        @Schema(description = "묶을 메뉴 ID 배열 (1개 이상)", example = "[7, 8]")
                @NotEmpty List<Long> menuIds,
        @Schema(description = "모집 시작 시각 (생략 시 현재)") @JsonFormat(shape = JsonFormat.Shape.STRING)
                LocalDateTime recruitmentStart,
        @Schema(description = "모집 마감 시각") @NotNull
                @JsonFormat(shape = JsonFormat.Shape.STRING)
                LocalDateTime recruitmentDeadline,
        @Schema(description = "1인당 최소 주문 수량", example = "1")
                @NotNull @Min(1) Integer minOrderPerPerson,
        @Schema(description = "결제 방식", example = "CARD") String paymentMethod,
        @Schema(description = "모집 정원", example = "10") @NotNull @Min(1) Integer participantTotal) {}
