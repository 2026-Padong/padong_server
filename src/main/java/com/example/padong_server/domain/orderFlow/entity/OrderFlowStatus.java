package com.example.padong_server.domain.orderFlow.entity;

public enum OrderFlowStatus {
    /** 모집 중. closingSoon 플래그로 임박(24h) 표시. */
    PENDING,
    /** 정원/시간 만료 또는 사장 승인 대기. */
    WAITING_APPROVAL,
    /** 사장 승인. 준비 중. */
    APPROVED,
    /** 픽업 준비 완료. */
    READY,
    /** 픽업 완료. */
    COMPLETED,
    /** 사장 거절 또는 모임 취소. */
    REJECTED
}
