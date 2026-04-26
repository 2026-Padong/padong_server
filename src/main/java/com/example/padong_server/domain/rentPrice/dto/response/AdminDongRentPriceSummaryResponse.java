package com.example.padong_server.domain.rentPrice.dto.response;

public record AdminDongRentPriceSummaryResponse(
        String adminDongCode,
        String periodLabel,
        ResidenceBuildingTypeResponse buildingType,
        RentPriceTradeTypeResponse tradeType,
        RentPriceDisplayValueResponse sale,
        RentPriceDisplayValueResponse jeonse,
        MonthlyRentDisplayValueResponse monthlyRent
) {
}
