package com.example.padong_server.domain.population.service;

import com.example.padong_server.domain.dongne.dto.AdminDongCsvRow;
import com.example.padong_server.domain.dongne.entity.AdminDong;
import com.example.padong_server.domain.dongne.service.DongneService;
import com.example.padong_server.domain.population.dto.response.PopulationDetailDto;
import com.example.padong_server.domain.population.entity.Population;
import com.example.padong_server.domain.population.repository.PopulationRepository;
import com.example.padong_server.domain.population.util.DensityDataUtil;
import com.example.padong_server.domain.population.util.PopulationDataUtil;
import com.example.padong_server.global.ResponseDTO;
import com.example.padong_server.global.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PopulationServiceTest {

    @Mock
    private PopulationRepository populationRepository;

    @Mock
    private PopulationDataUtil populationDataUtil;

    @Mock
    private DensityDataUtil densityDataUtil;

    @Mock
    private DongneService dongneService;

    private PopulationService populationService;

    @BeforeEach
    void setUp() {
        populationService = new PopulationService(
                populationRepository,
                populationDataUtil,
                densityDataUtil,
                dongneService
        );
    }

    @Test
    @DisplayName("행정동 코드로 인구 정보와 축구장 기준 인구수를 조회한다")
    void getPopulationDetailByAdminDongCode_returnsPopulationData() {
        AdminDong adminDong = adminDong("1111053000", "사직동");
        Population population = Population.builder()
                .adminDong(adminDong)
                .totalPopulation(8_893)
                .density(7_230.081301)
                .build();
        when(dongneService.findAdminDongByCode("1111053000")).thenReturn(adminDong);
        when(populationRepository.findByAdminDong(adminDong)).thenReturn(Optional.of(population));

        ResponseDTO<PopulationDetailDto> response = populationService.getPopulationDetailByAdminDongCode("1111053000");

        assertThat(response.getStatusCode()).isEqualTo("200");
        assertThat(response.getMessage()).isEqualTo("population 조회 성공");
        assertThat(response.getData().getDongneCode()).isEqualTo("1111053000");
        assertThat(response.getData().getDensity()).isEqualTo(7_230.081301);
        assertThat(response.getData().getSoccerFieldPopulation()).isEqualTo(51.62);
    }

    @Test
    @DisplayName("인구 정보가 없으면 not found 예외를 던진다")
    void getPopulationDetailByAdminDongCode_throwsWhenPopulationMissing() {
        AdminDong adminDong = adminDong("1111053000", "사직동");
        when(dongneService.findAdminDongByCode("1111053000")).thenReturn(adminDong);
        when(populationRepository.findByAdminDong(adminDong)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> populationService.getPopulationDetailByAdminDongCode("1111053000"))
                .isInstanceOf(CustomException.class)
                .hasMessage("해당 행정동의 인구 정보가 없습니다.");
    }

    private AdminDong adminDong(String adminDongCode, String adminDongName) {
        return new AdminDong(new AdminDongCsvRow(
                adminDongCode,
                "서울특별시",
                "종로구",
                adminDongName,
                37.58,
                126.97,
                "20081101",
                ""
        ));
    }
}
