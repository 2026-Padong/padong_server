package com.example.padong_server.domian.population.service;

import com.example.padongbe.domain.dongne.entity.AdminDong;
import com.example.padongbe.domain.population.dto.response.PopulationDataDto;
import com.example.padongbe.domain.population.entity.Population;
import com.example.padongbe.domain.population.repository.PopulationRepository;
import com.example.padongbe.domain.population.util.DensityDataUtil;
import com.example.padongbe.domain.population.util.PopulationDataUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PopulationService {

    private final PopulationRepository populationRepository;
    private final PopulationDataUtil populationDataUtil;
    private final DensityDataUtil densityDataUtil;

    public void uploadPopulationData() {
        List<Population> populationList = populationDataUtil.readPopulationFromCsv();
        log.info(String.valueOf(populationList.size()));
        populationRepository.saveAll(populationList);
    }

    public PopulationDataDto getPopulationByAdmin(AdminDong adminDong) {
        Optional<Population> optional= populationRepository.findByAdminDong(adminDong);
      return optional.map(PopulationDataDto::new).orElse(null);
    }

    public void uploadDensityData() {
        densityDataUtil.readDensityFromExcel();
    }
}
