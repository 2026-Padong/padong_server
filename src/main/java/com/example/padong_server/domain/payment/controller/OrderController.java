package com.example.padong_server.domain.payment.controller;

import com.example.padong_server.domain.oauth.entity.CustomUserDetails;
import com.example.padong_server.domain.payment.dto.OrderResponse;
import com.example.padong_server.domain.payment.service.OrderQueryService;
import com.example.padong_server.global.CursorPageResponse;
import com.example.padong_server.global.ResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "주문", description = "내 주문 목록·상세 조회. 결제 흐름 자체는 /payments 도메인 참조.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/orders")
public class OrderController {

    private final OrderQueryService orderQueryService;

    @Operation(
            summary = "내 주문 목록 (커서)",
            description =
                    "본인 주문만 반환. 최신순(Order PK DESC) 커서 페이징. "
                            + "응답 nextCursor 를 다음 호출 cursor 로 사용. items 각 element 는 가게/메뉴 inline join.")
    @SecurityRequirement(name = "bearer-jwt")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "401", description = "accessToken 누락/만료"),
    })
    @GetMapping("/me")
    public ResponseEntity<ResponseDTO<CursorPageResponse<OrderResponse>>> getMyOrders(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "직전 페이지 마지막 orderId. 첫 페이지는 생략", example = "42")
                    @RequestParam(required = false)
                    Long cursor,
            @Parameter(description = "페이지 크기 (기본 20, 최대 50)", example = "20")
                    @RequestParam(required = false, defaultValue = "20")
                    int size) {
        return ResponseEntity.ok(
                ResponseDTO.res(
                        HttpStatus.OK,
                        "주문 목록 조회 성공",
                        orderQueryService.getMyOrders(userDetails.getUserId(), cursor, size)));
    }

    @Operation(
            summary = "주문 상세",
            description = "본인 주문만 조회 가능. 다른 사람 주문 요청 시 404 (존재 노출 방지).")
    @SecurityRequirement(name = "bearer-jwt")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "401", description = "accessToken 누락/만료"),
        @ApiResponse(responseCode = "404", description = "주문 없음 또는 본인 주문 아님"),
    })
    @GetMapping("/{orderId}")
    public ResponseEntity<ResponseDTO<OrderResponse>> getOrder(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "주문 PK", example = "42") @PathVariable Long orderId) {
        return ResponseEntity.ok(
                ResponseDTO.res(
                        HttpStatus.OK,
                        "주문 상세 조회 성공",
                        orderQueryService.getOrder(userDetails.getUserId(), orderId)));
    }
}
