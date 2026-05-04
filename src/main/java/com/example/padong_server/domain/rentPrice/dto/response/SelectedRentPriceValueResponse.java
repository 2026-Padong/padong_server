package com.example.padong_server.domain.rentPrice.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        description = "선택 거래유형의 대표 가격",
        oneOf = {RentPriceDisplayValueResponse.class, MonthlyRentDisplayValueResponse.class})
public interface SelectedRentPriceValueResponse {}
