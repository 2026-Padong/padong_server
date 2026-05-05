package com.example.padong_server.domain.hotplace.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HotplacePopulation {

    private final String min;
    private final String max;
    private final String display;
    private final String congestionLevel;
    private final String congestionMessage;
    private final PopulationForecast forecast;
}
