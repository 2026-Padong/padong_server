package com.example.padong_server.domain.rentPrice.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RentPriceDto {
    private String buildingType;
    private Long avgJeonseDeposit;
    private Long avgMonthlyDeposit;
    private Long avgMonthlyRent;

}
