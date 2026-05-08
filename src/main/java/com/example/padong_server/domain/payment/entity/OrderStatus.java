package com.example.padong_server.domain.payment.entity;

public enum OrderStatus {
    READY,      // 아직 결제 전/검증 전
    PAID,       // 결제 완료
    CANCELED,   // 취소 완료
    FAILED      // 결제 실패
}
