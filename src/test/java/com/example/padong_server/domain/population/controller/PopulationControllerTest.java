package com.example.padong_server.domain.population.controller;

import com.example.padong_server.domain.population.dto.response.PopulationDetailDto;
import com.example.padong_server.domain.population.entity.Population;
import com.example.padong_server.domain.population.service.PopulationService;
import com.example.padong_server.global.ResponseDTO;
import com.example.padong_server.global.exception.CustomException;
import com.example.padong_server.global.exception.ErrorCode;
import com.example.padong_server.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PopulationControllerTest {

    @Mock
    private PopulationService populationService;

    @InjectMocks
    private PopulationController populationController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(populationController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("행정동별 인구밀도와 축구장 기준 인구수를 조회한다")
    void getPopulationDetail_returnsPopulationResponse() throws Exception {
        PopulationDetailDto dto = new PopulationDetailDto(populationWithCode("1111053000", 7_230.081301));
        ResponseDTO<PopulationDetailDto> response = ResponseDTO.res(
                org.springframework.http.HttpStatus.OK,
                "population 조회 성공",
                dto
        );
        given(populationService.getPopulationDetailByAdminDongCode("1111053000")).willReturn(response);

        mockMvc.perform(get("/population/detail")
                        .param("dongneCode", "1111053000")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("200"))
                .andExpect(jsonPath("$.message").value("population 조회 성공"))
                .andExpect(jsonPath("$.data.dongneCode").value("1111053000"))
                .andExpect(jsonPath("$.data.density").value(7230.081301))
                .andExpect(jsonPath("$.data.soccerFieldPopulation").value(51.62));
    }

    @Test
    @DisplayName("행정동 인구 정보가 없으면 공통 에러 응답을 반환한다")
    void getPopulationDetail_returnsErrorResponse() throws Exception {
        given(populationService.getPopulationDetailByAdminDongCode("9999999999"))
                .willThrow(new CustomException(ErrorCode.POPULATION_NOT_FOUND));

        mockMvc.perform(get("/population/detail")
                        .param("dongneCode", "9999999999")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POPULATION_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("해당 행정동의 인구 정보가 없습니다."));
    }

    private Population populationWithCode(String adminDongCode, double density) {
        return Population.builder()
                .adminDong(new com.example.padong_server.domain.dongne.entity.AdminDong(
                        new com.example.padong_server.domain.dongne.dto.AdminDongCsvRow(
                                adminDongCode,
                                "서울특별시",
                                "종로구",
                                "사직동",
                                37.58,
                                126.97,
                                "20081101",
                                ""
                        )
                ))
                .density(density)
                .build();
    }
}
