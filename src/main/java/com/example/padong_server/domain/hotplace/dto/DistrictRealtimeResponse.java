package com.example.padong_server.domain.hotplace.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class DistrictRealtimeResponse {

    private final String guName;
    private final String selectedAreaNm;
    private final WeatherSummary summary;
    private final List<HotplaceRealtimeItem> hotplaces;
}
