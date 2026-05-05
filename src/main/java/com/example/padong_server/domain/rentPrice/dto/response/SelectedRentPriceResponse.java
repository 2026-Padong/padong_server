package com.example.padong_server.domain.rentPrice.dto.response;

import com.example.padong_server.domain.rentPrice.entity.RentPriceTradeType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "선택 주거/가격 정보")
public record SelectedRentPriceResponse(
        @Schema(description = "선택 건물유형") ResidenceBuildingTypeResponse buildingType,
        @Schema(description = "선택 거래유형") RentPriceTradeTypeResponse tradeType,
        @Schema(
                        description = "선택 거래유형의 대표 가격. 단위는 만원.",
                        oneOf = {
                            RentPriceDisplayValueResponse.class,
                            MonthlyRentDisplayValueResponse.class
                        })
                SelectedRentPriceValueResponse price) {

    public static SelectedRentPriceResponse from(
            AdminDongRentPriceBuildingTypeResponse response, RentPriceTradeType tradeType) {
        SelectedRentPriceValueResponse price = selectedPrice(response, tradeType);
        if (response == null || price == null) {
            return null;
        }
        return new SelectedRentPriceResponse(
                response.buildingType(), toTradeTypeResponse(tradeType), price);
    }

    private static RentPriceTradeTypeResponse toTradeTypeResponse(RentPriceTradeType tradeType) {
        if (tradeType == null) {
            return null;
        }
        return new RentPriceTradeTypeResponse(tradeType.code(), tradeType.label());
    }

    private static SelectedRentPriceValueResponse selectedPrice(
            AdminDongRentPriceBuildingTypeResponse response, RentPriceTradeType tradeType) {
        if (response == null || tradeType == null) {
            return null;
        }

        return switch (tradeType) {
            case SALE -> {
                RentPriceDisplayValueResponse sale = response.sale();
                yield sale == null || sale.amount() == null ? null : sale;
            }
            case JEONSE -> {
                RentPriceDisplayValueResponse jeonse = response.jeonse();
                yield jeonse == null || jeonse.amount() == null ? null : jeonse;
            }
            case MONTHLY_RENT -> {
                MonthlyRentDisplayValueResponse monthlyRent = response.monthlyRent();
                yield monthlyRent == null
                                || monthlyRent.deposit() == null
                                || monthlyRent.monthlyRent() == null
                        ? null
                        : monthlyRent;
            }
        };
    }
}
