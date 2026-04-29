package com.example.padong_server.domain.hotplace.controller;

import com.example.padong_server.domain.hotplace.dto.DistrictRealtimeResponse;
import com.example.padong_server.domain.hotplace.dto.HotplaceRealtimeItem;
import com.example.padong_server.domain.hotplace.dto.WeatherSummary;
import com.example.padong_server.domain.hotplace.service.HotplaceRealtimeService;
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

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DistrictRealtimeControllerTest {

    @Mock
    private HotplaceRealtimeService hotplaceRealtimeService;

    @InjectMocks
    private DistrictRealtimeController districtRealtimeController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(districtRealtimeController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("자치구 실시간 조회 성공 시 응답 DTO를 반환한다")
    void getDistrictRealtime_returnsResponse() throws Exception {
        DistrictRealtimeResponse response = DistrictRealtimeResponse.builder()
                .guName("용산구")
                .selectedAreaNm("국립 중앙박물관")
                .summary(WeatherSummary.builder()
                        .weatherStatus("맑음")
                        .temperature("21.3")
                        .sensibleTemperature("22.0")
                        .humidity("55%")
                        .fineDustStatus("좋음")
                        .fineDust("18.0")
                        .precipitationProbability("10%")
                        .build())
                .hotplaces(List.of(
                        HotplaceRealtimeItem.builder()
                                .areaNm("국립 중앙박물관")
                                .thumbnail("https://example.com/museum.jpg")
                                .roadAddr("서울 용산구 서빙고로 137")
                                .areaPpltnMin("12000")
                                .areaPpltnMax("18000")
                                .congestionLevel("여유")
                                .dominantAgeGroup("20대")
                                .dominantAgeRate("31.2%")
                                .roadTrafficIdx("원활")
                                .roadTrafficSpd("42.5")
                                .build()
                ))
                .build();

        given(hotplaceRealtimeService.getDistrictRealtime("용산구")).willReturn(response);

        mockMvc.perform(get("/api/realtime/districts")
                        .param("guName", "용산구")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.guName").value("용산구"))
                .andExpect(jsonPath("$.selectedAreaNm").value("국립 중앙박물관"))
                .andExpect(jsonPath("$.summary.weatherStatus").value("맑음"))
                .andExpect(jsonPath("$.summary.temperature").value("21.3"))
                .andExpect(jsonPath("$.summary.sensibleTemperature").value("22.0"))
                .andExpect(jsonPath("$.summary.humidity").value("55%"))
                .andExpect(jsonPath("$.summary.fineDustStatus").value("좋음"))
                .andExpect(jsonPath("$.summary.fineDust").value("18.0"))
                .andExpect(jsonPath("$.summary.precipitationProbability").value("10%"))
                .andExpect(jsonPath("$.hotplaces[0].areaNm").value("국립 중앙박물관"))
                .andExpect(jsonPath("$.hotplaces[0].thumbnail").value("https://example.com/museum.jpg"))
                .andExpect(jsonPath("$.hotplaces[0].roadAddr").value("서울 용산구 서빙고로 137"))
                .andExpect(jsonPath("$.hotplaces[0].areaPpltnMin").value("12000"))
                .andExpect(jsonPath("$.hotplaces[0].areaPpltnMax").value("18000"))
                .andExpect(jsonPath("$.hotplaces[0].congestionLevel").value("여유"))
                .andExpect(jsonPath("$.hotplaces[0].dominantAgeGroup").value("20대"))
                .andExpect(jsonPath("$.hotplaces[0].dominantAgeRate").value("31.2%"))
                .andExpect(jsonPath("$.hotplaces[0].roadTrafficIdx").value("원활"))
                .andExpect(jsonPath("$.hotplaces[0].roadTrafficSpd").value("42.5"));
    }

    @Test
    @DisplayName("핫플레이스가 없으면 공통 에러 응답을 반환한다")
    void getDistrictRealtime_returnsErrorResponse() throws Exception {
        given(hotplaceRealtimeService.getDistrictRealtime("용산구"))
                .willThrow(new CustomException(ErrorCode.HOTPLACE_NOT_FOUND));

        mockMvc.perform(get("/api/realtime/districts")
                        .param("guName", "용산구")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("HOTPLACE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("해당 구에 등록된 핫플레이스가 없습니다."));
    }
}
