package com.example.padong_server.domain.payment.entity;

public enum OrderStatus {
    READY,                      // 아직 결제 전/검증 전
    PAID,                       // 결제 완료
    GROUP_PURCHASE_CONFIRMED,   // 공동구매 목표 달성 및 주문 확정
    CANCEL_PENDING,             // 주문 취소 처리 중
    CANCELED,                   // 주문 취소 완료
    PREPARING,                  // 상품 준비 중
    READY_FOR_PICKUP,           // 픽업 가능
    COMPLETED,                  // 픽업 및 주문 완료
    FAILED                      // 주문 실패
}
