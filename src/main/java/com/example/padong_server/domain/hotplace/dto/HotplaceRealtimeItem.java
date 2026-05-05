package com.example.padong_server.domain.hotplace.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HotplaceRealtimeItem {

    private final String areaNm;
    private final String thumbnail;
    private final String category;
    private final String roadAddr;
    private final String eventNm;
    private final WeatherSummary weather;
    private final HotplacePopulation population;
    private final HotplaceAge age;
    private final HotplaceGender gender;
    private final HotplaceTransport transport;
    private final RoadTrafficInfo roadTraffic;
}
