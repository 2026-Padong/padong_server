package com.example.padong_server.domain.activityMobility.dto;

public record SafetyIndexCsvRow(
        String cityName,
        String districtName,
        int trafficAccidentScore,
        int fireScore,
        int crimeScore,
        int lifeSafetyScore,
        int suicideScore,
        int infectiousDiseaseScore
) {
}
