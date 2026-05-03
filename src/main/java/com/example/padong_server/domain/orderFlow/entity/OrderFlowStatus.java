package com.example.padong_server.domain.orderFlow.entity;

public enum OrderFlowStatus {
    WAITING_APPROVAL,
    REJECTED,
    PREPARING,
    READY_FOR_PICKUP,
    PICKUP_COMPLETED
}
