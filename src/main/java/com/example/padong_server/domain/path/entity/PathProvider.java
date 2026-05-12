package com.example.padong_server.domain.path.entity;

/**
 * 경로 외부 API 공급자 식별자.
 * ErrorCode 에 부여해 fallback 분기를 타입 안전하게 처리한다.
 */
public enum PathProvider {
    ODSAY,
    SK_PEDESTRIAN,
    SK_CAR,
    GOOGLE
}
