package com.example.padong_server.domain.population.dto.response;

import com.example.padong_server.domain.population.entity.Population;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
public class PopulationDetailDto {

    private static final double SOCCER_FIELD_SQUARE_METERS = 7140.0;
    private static final double SQUARE_METERS_PER_SQUARE_KILOMETER = 1_000_000.0;

    private final String dongneCode;
    private final double density;
    private final double soccerFieldPopulation;

    public PopulationDetailDto(Population population) {
        this.dongneCode = population.getAdminDong().getAdminDongCode();
        this.density = population.getDensity();
        this.soccerFieldPopulation = round(density * (SOCCER_FIELD_SQUARE_METERS / SQUARE_METERS_PER_SQUARE_KILOMETER));
    }

    private static double round(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
