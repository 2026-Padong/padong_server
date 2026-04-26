package com.example.padong_server.domain.activityMobility.dto;

import com.example.padong_server.domain.dongne.dto.AdminDongDto;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class IntersectedMobilityResponse {
    private AdminDongDto departureDong;
    private double score;
    private double density;
    private double safety;
    private double avgJeonseDeposit;
    private double avgMonthlyDeposit;
    private double avgMonthlyRent;
    private double totalMobility1;
    private double totalMobility2;
    private double avgTime1;
    private double avgTime2;

    public static MobilityResponse toMobilityResponse(IntersectedMobilityResponse intersectedMobilityResponse) {
        return MobilityResponse.builder()
                .departureDong(intersectedMobilityResponse.getDepartureDong())
                .score(intersectedMobilityResponse.getScore())
                .density(intersectedMobilityResponse.getDensity())
                .safety(intersectedMobilityResponse.getSafety())
                .avgJeonseDeposit(intersectedMobilityResponse.getAvgJeonseDeposit())
                .avgMonthlyDeposit(intersectedMobilityResponse.getAvgMonthlyDeposit())
                .avgMonthlyRent(intersectedMobilityResponse.getAvgMonthlyRent())
                .totalMobility(Math.round((intersectedMobilityResponse.getTotalMobility1() + intersectedMobilityResponse.getTotalMobility2()) / 2.0 * 100.0) / 100.0)
                .avgTime(Math.round((intersectedMobilityResponse.getAvgTime1() + intersectedMobilityResponse.getAvgTime2()) / 2.0 * 100.0) / 100.0)
                .build();
    }
//    private double totalMobility;
//    private double avgTime;
}
