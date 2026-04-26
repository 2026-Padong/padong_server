package com.example.padong_server.domain.rentPrice.dto.response;

public record AdminDongRentPriceBuildingTypeResponse(
        ResidenceBuildingTypeResponse buildingType,
        RentPriceDisplayValueResponse sale,
        RentPriceDisplayValueResponse jeonse,
        MonthlyRentDisplayValueResponse monthlyRent
) {
}
