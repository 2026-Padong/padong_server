package com.example.padong_server.domain.dongne.dto;

public record LegalDongCsvRow(
        String legalDongCode,
        String cityName,
        String districtName,
        String legalDongName,
        String sourceDate,
        String deletedDate
) {
}
