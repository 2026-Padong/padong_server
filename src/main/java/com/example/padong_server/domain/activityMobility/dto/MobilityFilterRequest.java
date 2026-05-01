package com.example.padong_server.domain.activityMobility.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MobilityFilterRequest {

    private Double minAvgTime;
    private Double maxAvgTime;
    private Double minEachAvgTime;
    private Double maxEachAvgTime;
    private List<String> departureDistrictNames;
    private String contractType;
    private String houseType;
    private Long minSalePrice;
    private Long maxSalePrice;
    private Long minJeonseDeposit;
    private Long maxJeonseDeposit;
    private Long minMonthlyDeposit;
    private Long maxMonthlyDeposit;
    private Long minMonthlyRent;
    private Long maxMonthlyRent;

    public static MobilityFilterRequest empty() {
        return MobilityFilterRequest.builder().build();
    }
}
