package com.example.padong_server.domain.payment.entity;

public enum PaymentStatus {
    READY,      // 아직 결제 전/검증 전
    PAID,       // 결제 완료, 취소 가능
    CANCELED,   // 취소 완료
    FAILED;     // 결제 실패

    public boolean canCancel() {
        return this == PAID;
    }
}
