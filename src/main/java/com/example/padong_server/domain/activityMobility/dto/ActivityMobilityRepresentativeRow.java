package com.example.padong_server.domain.activityMobility.dto;

public record ActivityMobilityRepresentativeRow(
        String startMonth,
        String endMonth,
        String arrivalDongCode,
        String departureDongCode,
        double totalMobility,
        double avgTime,
        int observedMonthCount
) {
}
