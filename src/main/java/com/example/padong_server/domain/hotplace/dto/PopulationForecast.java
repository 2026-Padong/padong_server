package com.example.padong_server.domain.hotplace.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PopulationForecast {

    private final String status;
    private final String populationMin;
    private final String populationMax;
    private final String time;
}
