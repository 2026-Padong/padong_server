package com.example.padong_server.domain.hotplace.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HotplaceRealtimeItem {

    private final String areaNm;
    private final String thumbnail;
    private final String roadAddr;
    private final String areaPpltnMin;
    private final String areaPpltnMax;
    private final String congestionLevel;
    private final String dominantAgeGroup;
    private final String dominantAgeRate;
    private final String roadTrafficIdx;
    private final String roadTrafficSpd;
}
