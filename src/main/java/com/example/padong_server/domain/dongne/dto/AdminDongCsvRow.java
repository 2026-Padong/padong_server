package com.example.padong_server.domain.dongne.dto;

public record AdminDongCsvRow(
        String adminDongCode,
        String cityName,
        String districtName,
        String adminDongName,
        Double latitude,
        Double longitude,
        String sourceDate,
        String deletedDate
) {
}
