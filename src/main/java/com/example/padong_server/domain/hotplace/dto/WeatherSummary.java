package com.example.padong_server.domain.hotplace.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WeatherSummary {

    private final String weatherStatus;
    private final String temperature;
    private final String pm10;
    private final String precipitationProbability;
}
