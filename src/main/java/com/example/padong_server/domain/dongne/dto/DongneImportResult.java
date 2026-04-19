package com.example.padong_server.domain.dongne.dto;

public record DongneImportResult(
        int adminDongCount,
        int legalDongCount,
        int mappingCount
) {
}
