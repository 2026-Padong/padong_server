package com.example.padong_server.domain.dongne.dto;

public record DongMappingCsvRow(
        String adminDongCode,
        String cityName,
        String districtName,
        String adminDongName,
        String legalDongCode,
        String legalDongName,
        String sourceDate,
        String deletedDate
) {
}
