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

@Tag(name = "Order Flow", description = "APIs for store owners to manage the order status flow after menu recruitment is full")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/order-flows")
public class OrderFlowController {

    private final OrderFlowService orderFlowService;

    @Operation(
            summary = "Get order flow by menu",
            description = "Returns the latest order flow for the given menu. An order flow is created automatically when the menu reaches max participants."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order flow retrieved"),
            @ApiResponse(responseCode = "404", description = "Order flow not found")
    })
    @GetMapping
    public ResponseEntity<ResponseDTO<OrderFlowResponse>> getOrderFlow(
            @Parameter(description = "Menu ID", example = "1")
            @RequestParam Long menuId
    ) {
        OrderFlowResponse response = orderFlowService.getOrderFlowByMenu(menuId);
        return ResponseEntity.ok(ResponseDTO.res(HttpStatus.OK, "주문 상태 흐름 조회에 성공했습니다.", response));
    }

    @Operation(
            summary = "Approve order",
            description = "Changes the order flow from WAITING_APPROVAL to PREPARING."
    )
    @PutMapping("/approve")
    public ResponseEntity<ResponseDTO<OrderFlowResponse>> approve(
            @Parameter(description = "Order flow ID", example = "1")
            @RequestParam Long orderFlowId
    ) {
        OrderFlowResponse response = orderFlowService.approve(orderFlowId);
        return ResponseEntity.ok(ResponseDTO.res(HttpStatus.OK, "접수 수락이 완료되었습니다.", response));
    }

    @Operation(
            summary = "Reject order",
            description = "Changes the order flow from WAITING_APPROVAL to REJECTED."
    )
    @PutMapping("/reject")
    public ResponseEntity<ResponseDTO<OrderFlowResponse>> reject(
            @Parameter(description = "Order flow ID", example = "1")
            @RequestParam Long orderFlowId
    ) {
        OrderFlowResponse response = orderFlowService.reject(orderFlowId);
        return ResponseEntity.ok(ResponseDTO.res(HttpStatus.OK, "접수 거절이 완료되었습니다.", response));
    }

    @Operation(
            summary = "Mark ready for pickup",
            description = "Changes the order flow from PREPARING to READY_FOR_PICKUP."
    )
    @PutMapping("/ready")
    public ResponseEntity<ResponseDTO<OrderFlowResponse>> markReadyForPickup(
            @Parameter(description = "Order flow ID", example = "1")
            @RequestParam Long orderFlowId
    ) {
        OrderFlowResponse response = orderFlowService.markReadyForPickup(orderFlowId);
        return ResponseEntity.ok(ResponseDTO.res(HttpStatus.OK, "픽업 준비 완료로 변경되었습니다.", response));
    }

    @Operation(
            summary = "Complete pickup",
            description = "Changes the order flow from READY_FOR_PICKUP to PICKUP_COMPLETED."
    )
    @PutMapping("/pickup-complete")
    public ResponseEntity<ResponseDTO<OrderFlowResponse>> completePickup(
            @Parameter(description = "Order flow ID", example = "1")
            @RequestParam Long orderFlowId
    ) {
        OrderFlowResponse response = orderFlowService.completePickup(orderFlowId);
        return ResponseEntity.ok(ResponseDTO.res(HttpStatus.OK, "픽업 완료로 변경되었습니다.", response));
    }
}
