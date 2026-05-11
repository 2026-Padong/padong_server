package com.example.padong_server.domain.payment.dto;

public record PaymentPrepareResponse(
        Long orderId,
        String paymentId,
        Long amount,
        String orderName,
        String customerName,
        boolean isTest
) {
}
