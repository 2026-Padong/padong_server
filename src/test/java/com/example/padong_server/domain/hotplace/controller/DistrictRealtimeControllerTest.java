package com.example.padong_server.domain.hotplace.controller;

import com.example.padong_server.domain.hotplace.dto.AgeDetail;
import com.example.padong_server.domain.hotplace.dto.AgeDistributionItem;
import com.example.padong_server.domain.hotplace.dto.DistrictRealtimeResponse;
import com.example.padong_server.domain.hotplace.dto.HotplaceAge;
import com.example.padong_server.domain.hotplace.dto.HotplaceGender;
import com.example.padong_server.domain.hotplace.dto.HotplacePopulation;
import com.example.padong_server.domain.hotplace.dto.HotplaceRealtimeItem;
import com.example.padong_server.domain.hotplace.dto.HotplaceTransport;
import com.example.padong_server.domain.hotplace.dto.NamedCountInfo;
import com.example.padong_server.domain.hotplace.dto.PopulationForecast;
import com.example.padong_server.domain.hotplace.dto.RoadTrafficInfo;
import com.example.padong_server.domain.hotplace.dto.SubwayTransportInfo;
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
    @DisplayName("자치구 실시간 조회 성공 시 카드형 응답 DTO를 반환한다")
    void getDistrictRealtime_returnsResponse() throws Exception {
        DistrictRealtimeResponse response = DistrictRealtimeResponse.builder()
                .guName("종로구")
                .selectedAreaNm("광화문·덕수궁")
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
                                .areaNm("광화문·덕수궁")
                                .thumbnail("https://example.com/palace.jpg")
                                .category("고궁·문화유산")
                                .roadAddr("서울시 종로구·중구 일대")
                                .weather(WeatherSummary.builder()
                                        .weatherStatus("맑음")
                                        .temperature("21.3")
                                        .sensibleTemperature("22.0")
                                        .humidity("55%")
                                        .fineDustStatus("좋음")
                                        .fineDust("18.0")
                                        .precipitationProbability("10%")
                                        .build())
                                .population(HotplacePopulation.builder()
                                        .min("30000")
                                        .max("34000")
                                        .display("약 32,000명")
                                        .congestionLevel("여유")
                                        .congestionMessage("보행이 원활한 수준입니다.")
                                        .forecast(PopulationForecast.builder()
                                                .status("감소 예상")
                                                .populationMin("28000")
                                                .populationMax("31000")
                                                .time("2026-05-04T14:00:00")
                                                .build())
                                        .build())
                                .age(HotplaceAge.builder()
                                        .dominantGroup("30대")
                                        .dominantRate("23.8%")
                                        .top3(List.of(
                                                AgeDistributionItem.builder().ageGroup("30대").rate("23.8%").build(),
                                                AgeDistributionItem.builder().ageGroup("40대").rate("23.5%").build(),
                                                AgeDistributionItem.builder().ageGroup("20대").rate("18.8%").build()
                                        ))
                                        .detail(AgeDetail.builder()
                                                .rate10("11.2%")
                                                .rate20("18.8%")
                                                .rate30("23.8%")
                                                .rate40("23.5%")
                                                .rate50("14.1%")
                                                .rate60("8.6%")
                                                .rate70("0.0%")
                                                .build())
                                        .build())
                                .gender(HotplaceGender.builder()
                                        .maleRate("49.9%")
                                        .femaleRate("50.1%")
                                        .build())
                                .transport(HotplaceTransport.builder()
                                        .subway(SubwayTransportInfo.builder()
                                                .stationName("광화문역")
                                                .lines(List.of("5호선"))
                                                .stationCount(1)
                                                .build())
                                        .bus(NamedCountInfo.builder()
                                                .count(3)
                                                .names(List.of("세종문화회관", "광화문", "KT광화문지사"))
                                                .build())
                                        .bike(NamedCountInfo.builder()
                                                .count(6)
                                                .names(List.of("광화문역 2번 출구", "세종문화회관", "종로구청 앞"))
                                                .build())
                                        .build())
                                .roadTraffic(RoadTrafficInfo.builder()
                                        .status("원활")
                                        .speed("13km/h")
                                        .build())
                                .build()
                ))
                .build();

        given(hotplaceRealtimeService.getDistrictRealtime("종로구")).willReturn(response);

        mockMvc.perform(get("/realtime/districts")
                        .param("guName", "종로구")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.guName").value("종로구"))
                .andExpect(jsonPath("$.selectedAreaNm").value("광화문·덕수궁"))
                .andExpect(jsonPath("$.summary.weatherStatus").value("맑음"))
                .andExpect(jsonPath("$.hotplaces[0].areaNm").value("광화문·덕수궁"))
                .andExpect(jsonPath("$.hotplaces[0].category").value("고궁·문화유산"))
                .andExpect(jsonPath("$.hotplaces[0].weather.temperature").value("21.3"))
                .andExpect(jsonPath("$.hotplaces[0].population.display").value("약 32,000명"))
                .andExpect(jsonPath("$.hotplaces[0].age.top3[0].ageGroup").value("30대"))
                .andExpect(jsonPath("$.hotplaces[0].gender.femaleRate").value("50.1%"))
                .andExpect(jsonPath("$.hotplaces[0].transport.subway.stationName").value("광화문역"))
                .andExpect(jsonPath("$.hotplaces[0].transport.subway.lines[0]").value("5호선"))
                .andExpect(jsonPath("$.hotplaces[0].transport.bus.count").value(3))
                .andExpect(jsonPath("$.hotplaces[0].transport.bus.names[1]").value("광화문"))
                .andExpect(jsonPath("$.hotplaces[0].roadTraffic.speed").value("13km/h"));
    }

    @Test
    @DisplayName("핫플레이스가 없으면 공통 에러 응답을 반환한다")
    void getDistrictRealtime_returnsErrorResponse() throws Exception {
        given(hotplaceRealtimeService.getDistrictRealtime("종로구"))
                .willThrow(new CustomException(ErrorCode.HOTPLACE_NOT_FOUND));

        mockMvc.perform(get("/realtime/districts")
                        .param("guName", "종로구")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("HOTPLACE_NOT_FOUND"));
    }
}
