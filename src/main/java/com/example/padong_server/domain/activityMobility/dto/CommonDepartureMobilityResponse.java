package com.example.padong_server.domain.activityMobility.dto;

import com.example.padong_server.domain.dongne.dto.AdminDongDto;
import com.example.padong_server.domain.dongne.entity.AdminDong;
import com.example.padong_server.domain.rentPrice.dto.response.SelectedRentPriceResponse;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "다중 행정동 공통 출발 후보 응답 item")
public class CommonDepartureMobilityResponse {

    @Schema(description = "공통 출발 후보 행정동")
    private AdminDongDto departureDong;

    @Schema(description = "입력한 여러 도착 행정동 기준 합산 생활이동 대표값", example = "400.58")
    private double totalMobility;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "선택 주거/가격 정보")
    private SelectedRentPriceResponse rentPrice;

    public static CommonDepartureMobilityResponse from(
            AdminDong departureDong, double totalMobility, SelectedRentPriceResponse rentPrice) {
        return CommonDepartureMobilityResponse.builder()
                .departureDong(AdminDongDto.from(departureDong))
                .totalMobility(roundToSecondDecimal(totalMobility))
                .rentPrice(rentPrice)
                .build();
    }

    private static double roundToSecondDecimal(double value) {
        return Math.round(value * 100) / 100.0;
    }
}
