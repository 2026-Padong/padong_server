package com.example.padong_server.domain.activityMobility.dto;

import com.example.padong_server.domain.activityMobility.entity.Mobility;
import com.example.padong_server.domain.dongne.dto.AdminDongDto;
import com.example.padong_server.domain.dongne.entity.AdminDong;
import com.example.padong_server.domain.rentPrice.dto.response.SelectedRentPriceResponse;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "단일 행정동 생활이동 조회 응답 item")
public class MobilitySimpleResponse {

    @Schema(description = "출발 후보 행정동")
    private AdminDongDto departureDong;

    @Schema(description = "최근 3개월 평일 일평균 생활이동 대표값", example = "477.07")
    private double totalMobility;

    @Schema(description = "평균 이동시간", example = "42.7")
    private double avgTime;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "선택 주거/가격 정보")
    private SelectedRentPriceResponse rentPrice;

    public static MobilitySimpleResponse from(
            Mobility mobility, SelectedRentPriceResponse rentPrice) {
        AdminDong departureDong = mobility.getDepartureDong();
        return MobilitySimpleResponse.builder()
                .departureDong(AdminDongDto.from(departureDong))
                .totalMobility(roundToSecondDecimal(mobility.getTotalMobility()))
                .avgTime(roundToSecondDecimal(mobility.getAvgTime()))
                .rentPrice(rentPrice)
                .build();
    }

    private static double roundToSecondDecimal(double value) {
        return Math.round(value * 100) / 100.0;
    }
}
