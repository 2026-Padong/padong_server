package com.example.padong_server.domain.activityMobility.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MultiMobilityResponse {
    private List<MobilityResponse> firstMobility;
    private List<MobilityResponse> secondMobility;
    private List<MobilityResponse> intersectedMobility;
}
