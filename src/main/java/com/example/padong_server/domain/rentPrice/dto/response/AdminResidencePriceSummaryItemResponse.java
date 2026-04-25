package com.example.padong_server.domain.rentPrice.dto.response;

public record AdminResidencePriceSummaryItemResponse(
        String adminDongCode,
        String periodLabel,
        ResidenceTypeResponse dominantResidenceType,
        ResidenceDisplayValueResponse sale,
        ResidenceDisplayValueResponse jeonse,
        MonthlyRentDisplayValueResponse monthlyRent
) {
}
