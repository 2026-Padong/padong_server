package com.example.padong_server.domain.payment.entity;

public enum PaymentStatus {
    READY,              // 아직 결제 전/검증 전
    PROCESSING,         // 결제 확정 처리 중
    PAID,               // 결제 완료, 취소 가능
    CANCEL_PENDING,     // 결제 취소 처리 중
    CANCELED,           // 결제 취소 완료
    CANCEL_FAILED,      // 결제 취소 실패
    FAILED;             // 결제 실패

    public boolean canCancel() {
        return this == PAID;
    }
}
