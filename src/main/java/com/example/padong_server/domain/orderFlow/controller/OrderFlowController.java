package com.example.padong_server.domain.orderFlow.controller;

import com.example.padong_server.domain.orderFlow.dto.OrderFlowResponse;
import com.example.padong_server.domain.orderFlow.service.OrderFlowService;
import com.example.padong_server.global.ResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "주문 흐름", description = "메뉴 모집 완료 후 주문 상태 흐름을 관리하는 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/order-flows")
public class OrderFlowController {

    private final OrderFlowService orderFlowService;

    @Operation(
            summary = "메뉴별 주문 흐름 조회",
            description = "해당 메뉴의 최신 주문 흐름을 조회합니다. 주문 흐름은 메뉴가 최대 참여 인원에 도달했을 때 자동 생성됩니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "주문 흐름 조회 성공"),
            @ApiResponse(responseCode = "404", description = "주문 흐름을 찾을 수 없음")
    })
    @GetMapping
    public ResponseEntity<ResponseDTO<OrderFlowResponse>> getOrderFlow(
            @Parameter(description = "메뉴 ID", example = "1")
            @RequestParam Long menuId
    ) {
        OrderFlowResponse response = orderFlowService.getOrderFlowByMenu(menuId);
        return ResponseEntity.ok(ResponseDTO.res(HttpStatus.OK, "주문 상태 흐름 조회에 성공했습니다.", response));
    }

    @Operation(
            summary = "주문 승인",
            description = "주문 흐름 상태를 WAITING_APPROVAL에서 PREPARING으로 변경합니다."
    )
    @PutMapping("/approve")
    public ResponseEntity<ResponseDTO<OrderFlowResponse>> approve(
            @Parameter(description = "주문 흐름 ID", example = "1")
            @RequestParam Long orderFlowId
    ) {
        OrderFlowResponse response = orderFlowService.approve(orderFlowId);
        return ResponseEntity.ok(ResponseDTO.res(HttpStatus.OK, "주문 승인이 완료되었습니다.", response));
    }

    @Operation(
            summary = "주문 거절",
            description = "주문 흐름 상태를 WAITING_APPROVAL에서 REJECTED로 변경합니다."
    )
    @PutMapping("/reject")
    public ResponseEntity<ResponseDTO<OrderFlowResponse>> reject(
            @Parameter(description = "주문 흐름 ID", example = "1")
            @RequestParam Long orderFlowId
    ) {
        OrderFlowResponse response = orderFlowService.reject(orderFlowId);
        return ResponseEntity.ok(ResponseDTO.res(HttpStatus.OK, "주문 거절이 완료되었습니다.", response));
    }

    @Operation(
            summary = "픽업 준비 완료 처리",
            description = "주문 흐름 상태를 PREPARING에서 READY_FOR_PICKUP으로 변경합니다."
    )
    @PutMapping("/ready")
    public ResponseEntity<ResponseDTO<OrderFlowResponse>> markReadyForPickup(
            @Parameter(description = "주문 흐름 ID", example = "1")
            @RequestParam Long orderFlowId
    ) {
        OrderFlowResponse response = orderFlowService.markReadyForPickup(orderFlowId);
        return ResponseEntity.ok(ResponseDTO.res(HttpStatus.OK, "픽업 준비 완료 상태로 변경되었습니다.", response));
    }

    @Operation(
            summary = "픽업 완료 처리",
            description = "주문 흐름 상태를 READY_FOR_PICKUP에서 PICKUP_COMPLETED로 변경합니다."
    )
    @PutMapping("/pickup-complete")
    public ResponseEntity<ResponseDTO<OrderFlowResponse>> completePickup(
            @Parameter(description = "주문 흐름 ID", example = "1")
            @RequestParam Long orderFlowId
    ) {
        OrderFlowResponse response = orderFlowService.completePickup(orderFlowId);
        return ResponseEntity.ok(ResponseDTO.res(HttpStatus.OK, "픽업 완료 상태로 변경되었습니다.", response));
    }
}
