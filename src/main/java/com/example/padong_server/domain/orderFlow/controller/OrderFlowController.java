package com.example.padong_server.domain.orderFlow.controller;

import com.example.padong_server.domain.oauth.entity.CustomUserDetails;
import com.example.padong_server.domain.orderFlow.dto.OrderFlowCreateRequest;
import com.example.padong_server.domain.orderFlow.dto.OrderFlowParticipantResponse;
import com.example.padong_server.domain.orderFlow.dto.OrderFlowResponse;
import com.example.padong_server.domain.orderFlow.service.OrderFlowService;
import com.example.padong_server.global.ResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "주문 흐름 / 모임", description = "모임(공구) 생성·조회·취소·상태 전이 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/order-flows")
public class OrderFlowController {

    private final OrderFlowService orderFlowService;

    @Operation(summary = "모임 생성", description = "메뉴 묶음으로 새 모임 생성. 같은 가게에 활성 모임 있으면 409.")
    @PostMapping
    public ResponseEntity<ResponseDTO<OrderFlowResponse>> createOrderFlow(
            @Valid @RequestBody OrderFlowCreateRequest request) {
        OrderFlowResponse response = orderFlowService.createOrderFlow(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseDTO.res(HttpStatus.CREATED, "모임 생성 성공", response));
    }

    @Operation(
            summary = "모임 조회",
            description = "menuId 또는 storeId 둘 중 하나 필수. menuId: 해당 메뉴 최신 모임. storeId: 가게 활성 모임 단건.")
    @GetMapping
    public ResponseEntity<ResponseDTO<OrderFlowResponse>> getOrderFlow(
            @Parameter(description = "메뉴 ID") @RequestParam(required = false) Long menuId,
            @Parameter(description = "가게 ID — 활성 모임 단건 조회") @RequestParam(required = false)
                    Long storeId) {
        OrderFlowResponse response =
                storeId != null
                        ? orderFlowService.getActiveByStore(storeId)
                        : orderFlowService.getOrderFlowByMenu(menuId);
        return ResponseEntity.ok(ResponseDTO.res(HttpStatus.OK, "모임 조회 성공", response));
    }

    @Operation(summary = "모임 단건 조회")
    @GetMapping("/{orderFlowId}")
    public ResponseEntity<ResponseDTO<OrderFlowResponse>> getById(
            @PathVariable Long orderFlowId) {
        return ResponseEntity.ok(
                ResponseDTO.res(HttpStatus.OK, "모임 조회 성공", orderFlowService.getById(orderFlowId)));
    }

    @Operation(summary = "모임 참여자 목록", description = "결제·참여 도메인 미연결 — 현재 빈 리스트 반환.")
    @GetMapping("/{orderFlowId}/participants")
    public ResponseEntity<ResponseDTO<List<OrderFlowParticipantResponse>>> getParticipants(
            @PathVariable Long orderFlowId) {
        return ResponseEntity.ok(
                ResponseDTO.res(
                        HttpStatus.OK,
                        "참여자 조회 성공",
                        orderFlowService.getParticipants(orderFlowId)));
    }

    @Operation(
            summary = "모임 내역 (완료/거절/취소)",
            description =
                    "storeId, from, to 모두 optional. storeId 미입력 시 인증 ADMIN 본인 소유 가게 전체. "
                            + "from/to 미입력 시 기간 무제한.")
    @GetMapping("/history")
    public ResponseEntity<ResponseDTO<List<OrderFlowResponse>>> getHistory(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime from,
            @RequestParam(required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime to) {
        Long ownerId = userDetails == null ? null : userDetails.getUserId();
        return ResponseEntity.ok(
                ResponseDTO.res(
                        HttpStatus.OK,
                        "모임 내역 조회 성공",
                        orderFlowService.getHistory(storeId, ownerId, from, to)));
    }

    @Operation(summary = "모임 내역 단건")
    @GetMapping("/history/{orderFlowId}")
    public ResponseEntity<ResponseDTO<OrderFlowResponse>> getHistoryDetail(
            @PathVariable Long orderFlowId) {
        return ResponseEntity.ok(
                ResponseDTO.res(
                        HttpStatus.OK, "모임 내역 단건 조회 성공", orderFlowService.getById(orderFlowId)));
    }

    @Operation(
            summary = "모임 취소",
            description =
                    "RECRUITING/CLOSING/PENDING_FULL/WAITING_APPROVAL 상태에서만 가능. "
                            + "성공 시 응답 status = 'CANCELED' (REJECTED 와 구분되는 별도 상태).")
    @PutMapping("/cancel")
    public ResponseEntity<ResponseDTO<OrderFlowResponse>> cancel(
            @RequestParam Long orderFlowId,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(
                ResponseDTO.res(HttpStatus.OK, "모임 취소 성공", orderFlowService.cancel(orderFlowId, reason)));
    }

    @Operation(summary = "모임 삭제 (path variant)")
    @DeleteMapping("/{orderFlowId}")
    public ResponseEntity<ResponseDTO<OrderFlowResponse>> cancelPath(
            @PathVariable Long orderFlowId,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(
                ResponseDTO.res(HttpStatus.OK, "모임 취소 성공", orderFlowService.cancel(orderFlowId, reason)));
    }

    @Operation(summary = "사장 승인 (WAITING_APPROVAL → APPROVED)")
    @PutMapping("/approve")
    public ResponseEntity<ResponseDTO<OrderFlowResponse>> approve(@RequestParam Long orderFlowId) {
        return ResponseEntity.ok(
                ResponseDTO.res(HttpStatus.OK, "주문 승인 성공", orderFlowService.approve(orderFlowId)));
    }

    @Operation(summary = "사장 거절")
    @PutMapping("/reject")
    public ResponseEntity<ResponseDTO<OrderFlowResponse>> reject(@RequestParam Long orderFlowId) {
        return ResponseEntity.ok(
                ResponseDTO.res(HttpStatus.OK, "주문 거절 성공", orderFlowService.reject(orderFlowId)));
    }

    @Operation(summary = "픽업 준비 완료")
    @PutMapping("/ready")
    public ResponseEntity<ResponseDTO<OrderFlowResponse>> markReady(@RequestParam Long orderFlowId) {
        return ResponseEntity.ok(
                ResponseDTO.res(
                        HttpStatus.OK, "픽업 준비 완료", orderFlowService.markReadyForPickup(orderFlowId)));
    }

    @Operation(summary = "픽업 완료")
    @PutMapping("/pickup-complete")
    public ResponseEntity<ResponseDTO<OrderFlowResponse>> completePickup(
            @RequestParam Long orderFlowId) {
        return ResponseEntity.ok(
                ResponseDTO.res(
                        HttpStatus.OK, "픽업 완료", orderFlowService.completePickup(orderFlowId)));
    }
}
