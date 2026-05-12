package com.example.padong_server.domain.storeRegistration.entity;

/**
 * 사용자 측 가게 카드/상세에 표시하는 모집 상태.
 * OrderFlowStatus (사장 측) 와 별개 — 영업 시간·활성 모임 여부까지 합쳐 산출.
 */
public enum RecruitmentStatus {
    /** 모집 중. */
    RECRUITING,
    /** 마감 임박 (정원 1자리 남음 등). */
    CLOSING_SOON,
    /** 모집 종료, 모임 준비/픽업 등 진행 중. 신규 참여 불가. */
    IN_PROGRESS,
    /** 활성 모임 없음. */
    NO_FLOW,
    /** 영업 시간 외 또는 휴무일. */
    OUT_OF_HOURS
}
