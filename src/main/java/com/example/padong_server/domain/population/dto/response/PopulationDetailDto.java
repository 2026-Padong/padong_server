package com.example.padong_server.domain.population.dto.response;

import com.example.padong_server.domain.population.entity.Population;
import com.example.padong_server.domain.population.entity.PopulationDensity;
import lombok.Getter;

@Getter
public class PopulationDetailDto {

    private final String dongneCode;
    private final double density;
    private final double soccerFieldPopulation;

    public PopulationDetailDto(Population population) {
        this(
                population.getAdminDong().getAdminDongCode(),
                population.getDensity(),
                0.0
        );
    }

    public PopulationDetailDto(PopulationDensity populationDensity) {
        this(
                populationDensity.getAdminDong().getAdminDongCode(),
                populationDensity.getDensity(),
                populationDensity.getSoccerFieldPopulation()
        );
    }

    public PopulationDetailDto(String dongneCode, double density, double soccerFieldPopulation) {
        this.dongneCode = dongneCode;
        this.density = density;
        this.soccerFieldPopulation = soccerFieldPopulation;
    }
}
