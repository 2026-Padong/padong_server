package com.example.padong_server.domain.population.service;

import com.example.padong_server.domain.dongne.entity.AdminDong;
import com.example.padong_server.domain.dongne.service.DongneService;
import com.example.padong_server.domain.population.dto.response.PopulationDataDto;
import com.example.padong_server.domain.population.dto.response.PopulationDetailDto;
import com.example.padong_server.domain.population.entity.Population;
import com.example.padong_server.domain.population.entity.PopulationDensity;
import com.example.padong_server.domain.population.repository.PopulationDensityRepository;
import com.example.padong_server.domain.population.repository.PopulationRepository;
import com.example.padong_server.domain.population.util.DensityDataUtil;
import com.example.padong_server.domain.population.util.PopulationDataUtil;
import com.example.padong_server.global.ResponseDTO;
import com.example.padong_server.global.exception.CustomException;
import com.example.padong_server.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PopulationService {

    private final PopulationRepository populationRepository;
    private final PopulationDensityRepository populationDensityRepository;
    private final PopulationDataUtil populationDataUtil;
    private final DensityDataUtil densityDataUtil;
    private final DongneService dongneService;

    public void uploadPopulationData() {
        List<Population> populationList = populationDataUtil.readPopulationFromCsv();
        log.info(String.valueOf(populationList.size()));
        populationRepository.saveAll(populationList);
    }

    public PopulationDataDto getPopulationByAdmin(AdminDong adminDong) {
        return findPopulationByAdmin(adminDong)
                .map(PopulationDataDto::new)
                .orElse(null);
    }

    public Optional<Population> findPopulationByAdmin(AdminDong adminDong) {
        Optional<Population> population = populationRepository.findByAdminDong(adminDong);
        Optional<PopulationDensity> populationDensity = populationDensityRepository.findByAdminDong(adminDong);

        if (population.isEmpty() && populationDensity.isEmpty()) {
            return Optional.empty();
        }

        double totalPopulation = population.map(Population::getTotalPopulation)
                .orElseGet(() -> populationDensity.map(PopulationDensity::getTotalPopulation).orElse(0.0));
        double density = populationDensity.map(PopulationDensity::getDensity)
                .orElseGet(() -> population.map(Population::getDensity).orElse(0.0));

        return Optional.of(Population.builder()
                .adminDong(adminDong)
                .totalPopulation(totalPopulation)
                .density(density)
                .build());
    }

    public ResponseDTO<PopulationDetailDto> getPopulationDetailByAdminDongCode(String adminDongCode) {
        AdminDong adminDong = dongneService.findAdminDongByCode(adminDongCode);
        PopulationDensity populationDensity = populationDensityRepository.findByAdminDong(adminDong)
                .orElseThrow(() -> new CustomException(ErrorCode.POPULATION_NOT_FOUND));

        return ResponseDTO.res(HttpStatus.OK, "population 조회 성공", new PopulationDetailDto(populationDensity));
    }

    public void uploadDensityData() {
        List<PopulationDensity> densityList = densityDataUtil.readDensityFromCsv();
        log.info(String.valueOf(densityList.size()));
        populationDensityRepository.deleteAllInBatch();
        populationDensityRepository.saveAll(densityList);
    }
}
