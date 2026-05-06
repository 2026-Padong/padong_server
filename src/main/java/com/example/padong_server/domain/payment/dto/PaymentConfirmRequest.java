package com.example.padong_server.domain.payment.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PaymentConfirmRequest {

    // 포트원 결제 식별자
    private String paymentId;
    private String paymentToken;
    // 결제 시도 식별자
    private String txId;

    // 결제 금액 검증용
    private Long totalAmount;

    private Boolean isTest;
}
