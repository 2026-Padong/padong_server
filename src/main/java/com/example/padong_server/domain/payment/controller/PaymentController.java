package com.example.padong_server.domain.payment.controller;

import com.example.padong_server.domain.oauth.entity.CustomUserDetails;
import com.example.padong_server.domain.payment.dto.PaymentCancelRequest;
import com.example.padong_server.domain.payment.dto.PaymentConfirmRequest;
import com.example.padong_server.domain.payment.dto.PaymentPrepareRequest;
import com.example.padong_server.domain.payment.dto.PaymentPrepareResponse;
import com.example.padong_server.domain.payment.dto.PaymentResponse;
import com.example.padong_server.domain.payment.service.PaymentService;
import com.example.padong_server.global.ResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "결제", description = "공구 참여 결제 준비·승인·취소·조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(
            summary = "결제 준비",
            description =
                    """
                    공구 참여 전 결제 정보 사전 생성. 클라이언트가 결제 모듈에 넘길 paymentId / totalAmount 발급.

                    서버 측 가드 (요청이 다음 중 하나면 400 으로 거절):
                    - **GROUP_ORDER_FULL** — 모집 정원 초과 (currentParticipants >= maxParticipants)
                    - **GROUP_ORDER_RECRUITMENT_CLOSED** — 모집 마감 시각 경과
                    - **SOLD_OUT_MENU** — 요청 메뉴 중 하나라도 sold-out (Menu.soldOut 또는 OrderFlowMenu.soldOut)
                    - **INVALID_GROUP_ORDER_MENU** — 공구에 포함되지 않은 메뉴 ID
                    - **MIN_ORDER_AMOUNT_NOT_MET** — 합계가 공구 최소 주문 금액 미달
                    - **INVALID_PAYMENT_REQUEST** — 메뉴/수량 누락 등 요청값 오류
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "결제 준비 성공"),
            @ApiResponse(responseCode = "400", description = "가드 위반 (모집 종료·품절·정원·최소액 등)"),
            @ApiResponse(responseCode = "401", description = "비로그인 / 토큰 만료"),
            @ApiResponse(responseCode = "404", description = "공구 또는 메뉴 미존재")
    })
    @SecurityRequirement(name = "bearer-jwt")
    @PostMapping("/prepare")
    public ResponseEntity<ResponseDTO<PaymentPrepareResponse>> prepare(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody PaymentPrepareRequest request
    ) {
        PaymentPrepareResponse response = paymentService.prepare(userDetails.getUser(), request);
        return ResponseEntity.ok(ResponseDTO.res(HttpStatus.OK, "결제 준비 성공", response));
    }

    @Operation(
            summary = "결제 승인",
            description = "PG 측 결제 완료 후 paymentId 로 서버 측 승인 처리. PG 결과·금액 정합성 검증.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "결제 승인 성공"),
            @ApiResponse(responseCode = "400", description = "정합성 위반"),
            @ApiResponse(responseCode = "404", description = "결제 미존재")
    })
    @SecurityRequirement(name = "bearer-jwt")
    @PostMapping("/confirm")
    public ResponseEntity<ResponseDTO<PaymentResponse>> confirm(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody PaymentConfirmRequest request
    ) {
        PaymentResponse response = paymentService.confirm(userDetails.getUser(), request);
        return ResponseEntity.ok(ResponseDTO.res(HttpStatus.OK, "결제 승인 처리 완료", response));
    }

    @Operation(
            summary = "결제 취소",
            description =
                    "사용자 측 결제 취소. body 의 cancelReason 은 optional — 비워서 호출 가능. "
                            + "사장 거절·모임 실패 시 자동 환불은 별도 백엔드 로직.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "결제 취소 성공"),
            @ApiResponse(responseCode = "400", description = "취소 불가 상태"),
            @ApiResponse(responseCode = "404", description = "결제 미존재")
    })
    @SecurityRequirement(name = "bearer-jwt")
    @PostMapping("/{paymentId}/cancel")
    public ResponseEntity<ResponseDTO<PaymentResponse>> cancel(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String paymentId,
            @RequestBody PaymentCancelRequest request
    ) {
        PaymentResponse response = paymentService.cancel(userDetails.getUser(), paymentId, request);
        return ResponseEntity.ok(ResponseDTO.res(HttpStatus.OK, "결제 취소 성공", response));
    }

    @Operation(summary = "결제 단건 조회")
    @SecurityRequirement(name = "bearer-jwt")
    @GetMapping("/{paymentId}")
    public ResponseEntity<ResponseDTO<PaymentResponse>> getPayment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String paymentId
    ) {
        PaymentResponse response = paymentService.getPayment(userDetails.getUser(), paymentId);
        return ResponseEntity.ok(ResponseDTO.res(HttpStatus.OK, "결제 조회 성공", response));
    }
}
