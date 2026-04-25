package com.example.padong_server.domain.rentPrice.dto.response;

import java.util.List;

public record AdminResidencePriceDetailResponse(
        String adminDongCode,
        String periodLabel,
        String contractPeriodStart,
        String contractPeriodEnd,
        boolean excludedCancelledSales,
        ResidenceTypeResponse dominantResidenceType,
        List<BuildingTypeResidencePriceResponse> buildingTypes
) {
}
