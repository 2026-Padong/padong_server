package com.example.padong_server.domain.rentPrice.dto.request;

import com.example.padong_server.domain.rentPrice.entity.RentPriceTradeType;
import com.example.padong_server.domain.rentPrice.entity.ResidenceBuildingType;

public record RentPriceFilterCriteria(
        RentPriceTradeType tradeType,
        ResidenceBuildingType buildingType,
        Long minSalePrice,
        Long maxSalePrice,
        Long minJeonseDeposit,
        Long maxJeonseDeposit,
        Long minMonthlyDeposit,
        Long maxMonthlyDeposit,
        Long minMonthlyRent,
        Long maxMonthlyRent) {

    public static final RentPriceTradeType DEFAULT_TRADE_TYPE = RentPriceTradeType.MONTHLY_RENT;
    public static final ResidenceBuildingType DEFAULT_BUILDING_TYPE =
            ResidenceBuildingType.DETACHED_MULTIFAMILY;

    public static RentPriceFilterCriteria empty() {
        return from(null, null, null, null, null, null, null, null, null, null);
    }

    public static RentPriceFilterCriteria from(
            String tradeType,
            String buildingType,
            Long minSalePrice,
            Long maxSalePrice,
            Long minJeonseDeposit,
            Long maxJeonseDeposit,
            Long minMonthlyDeposit,
            Long maxMonthlyDeposit,
            Long minMonthlyRent,
            Long maxMonthlyRent) {
        return new RentPriceFilterCriteria(
                RentPriceTradeType.fromNullableOrDefault(tradeType, DEFAULT_TRADE_TYPE),
                ResidenceBuildingType.fromNullableOrDefault(buildingType, DEFAULT_BUILDING_TYPE),
                minSalePrice,
                maxSalePrice,
                minJeonseDeposit,
                maxJeonseDeposit,
                minMonthlyDeposit,
                maxMonthlyDeposit,
                minMonthlyRent,
                maxMonthlyRent);
    }
}
