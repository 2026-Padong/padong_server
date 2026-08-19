package com.example.padong_server.domain.payment.entity;

public enum GroupOrderStatus {
    DRAFT,
    OPEN,
    EVALUATING,
    CONFIRMED,
    CANCEL_PENDING,
    CANCELED,
    PREPARING,
    READY_FOR_PICKUP,
    COMPLETED
}
