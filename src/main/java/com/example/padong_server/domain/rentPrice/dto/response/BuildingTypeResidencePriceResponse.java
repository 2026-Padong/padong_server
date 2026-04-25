package com.example.padong_server.domain.rentPrice.dto.response;

public record BuildingTypeResidencePriceResponse(
        ResidenceTypeResponse buildingType,
        ResidenceDisplayValueResponse sale,
        ResidenceDisplayValueResponse jeonse,
        MonthlyRentDisplayValueResponse monthlyRent
) {
}
