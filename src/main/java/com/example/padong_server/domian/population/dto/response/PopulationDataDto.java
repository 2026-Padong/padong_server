package com.example.padong_server.domian.population.dto.response;

import com.example.padongbe.domain.population.entity.Population;
import lombok.Getter;

@Getter
public class PopulationDataDto {

    private String dongneCode;
    private double density;

    public PopulationDataDto(Population population) {
        this.dongneCode= population.getAdminDong().getAdminDongCode();
        this.density= population.getDensity();
    }

}
