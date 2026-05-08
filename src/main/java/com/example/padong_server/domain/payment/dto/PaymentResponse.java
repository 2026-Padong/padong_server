package com.example.padong_server.domain.payment.dto;

import com.example.padong_server.domain.payment.entity.Payment;
import com.example.padong_server.domain.payment.entity.PaymentStatus;
import java.time.LocalDateTime;

public record PaymentResponse(
        String paymentId,
        Long orderId,
        PaymentStatus status,
        Long totalAmount,
        Boolean isTest,
        String pgTxId,
        LocalDateTime paidAt,
        LocalDateTime canceledAt,
        String failureReason
) {

    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getPaymentId(),
                payment.getOrder().getId(),
                payment.getStatus(),
                payment.getTotalAmount(),
                payment.getIsTest(),
                payment.getPgTxId(),
                payment.getPaidAt(),
                payment.getCanceledAt(),
                payment.getFailureReason()
        );
    }
}
