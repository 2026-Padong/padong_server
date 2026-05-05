package com.example.padong_server.domain.activityMobility.dto;

public record ActivityMobilityCsvRow(
        String month,
        String arrivalDongCode,
        String departureDongCode,
        double sourceTotalMobility,
        double commuteInPopulation,
        double commuteOutPopulation,
        double avgTime
) {
}
