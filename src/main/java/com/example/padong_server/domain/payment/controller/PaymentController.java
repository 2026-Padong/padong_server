package com.example.padong_server.domain.payment.controller;

import com.example.padong_server.domain.oauth.entity.CustomUserDetails;
import com.example.padong_server.domain.payment.dto.PaymentCancelRequest;
import com.example.padong_server.domain.payment.dto.PaymentConfirmRequest;
import com.example.padong_server.domain.payment.dto.PaymentPrepareRequest;
import com.example.padong_server.domain.payment.dto.PaymentPrepareResponse;
import com.example.padong_server.domain.payment.dto.PaymentResponse;
import com.example.padong_server.domain.payment.service.PaymentService;
import com.example.padong_server.global.ResponseDTO;
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

@RestController
@RequiredArgsConstructor
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/prepare")
    public ResponseEntity<ResponseDTO<PaymentPrepareResponse>> prepare(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody PaymentPrepareRequest request
    ) {
        PaymentPrepareResponse response = paymentService.prepare(userDetails.getUser(), request);
        return ResponseEntity.ok(ResponseDTO.res(HttpStatus.OK, "결제 준비 성공", response));
    }

    @PostMapping("/confirm")
    public ResponseEntity<ResponseDTO<PaymentResponse>> confirm(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody PaymentConfirmRequest request
    ) {
        PaymentResponse response = paymentService.confirm(userDetails.getUser(), request);
        return ResponseEntity.ok(ResponseDTO.res(HttpStatus.OK, "결제 승인 처리 완료", response));
    }

    @PostMapping("/{paymentId}/cancel")
    public ResponseEntity<ResponseDTO<PaymentResponse>> cancel(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String paymentId,
            @RequestBody PaymentCancelRequest request
    ) {
        PaymentResponse response = paymentService.cancel(userDetails.getUser(), paymentId, request);
        return ResponseEntity.ok(ResponseDTO.res(HttpStatus.OK, "결제 취소 성공", response));
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<ResponseDTO<PaymentResponse>> getPayment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String paymentId
    ) {
        PaymentResponse response = paymentService.getPayment(userDetails.getUser(), paymentId);
        return ResponseEntity.ok(ResponseDTO.res(HttpStatus.OK, "결제 조회 성공", response));
    }
}
