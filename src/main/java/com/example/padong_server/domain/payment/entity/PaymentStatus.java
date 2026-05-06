package com.example.padong_server.domain.payment.entity;

public enum PaymentStatus {
    READY,      // 결제 준비
    PAID,       // 결제 완료
    FAILED,     // 결제 실패
    CANCELED    // 결제 취소
}
