package com.example.padong_server.domain.rentPrice.dto.response;

import java.util.List;

public record AdminDongRentPriceDetailResponse(
        String adminDongCode,
        String periodLabel,
        String contractPeriodStart,
        String contractPeriodEnd,
        boolean excludedCancelledSales,
        ResidenceBuildingTypeResponse dominantBuildingType,
        List<AdminDongRentPriceBuildingTypeResponse> buildingTypes
) {
}
