package com.example.padong_server.domain.hotplace.controller;

import com.example.padong_server.domain.hotplace.dto.DistrictSummaryResponse;
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
import java.util.stream.IntStream;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RealtimeDataControllerTest {

    @Mock
    private HotplaceRealtimeService hotplaceRealtimeService;

    @Mock
    private com.example.padong_server.domain.hotplace.service.HotPlaceService hotPlaceService;

    @InjectMocks
    private RealtimeDataController realtimeDataController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(realtimeDataController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("자치구 요약 조회 시 날씨·대표 POI 반환")
    void getDistrictSummary_returnsWeatherAndAreaNm() throws Exception {
        DistrictSummaryResponse response = DistrictSummaryResponse.builder()
                .guName("종로구")
                .selectedAreaNm("광화문·덕수궁")
                .summary(WeatherSummary.builder().weatherStatus("맑음").temperature("21.3").build())
                .build();
        given(hotplaceRealtimeService.getDistrictSummary("종로구")).willReturn(response);

        mockMvc.perform(get("/realtime/districts/종로구/summary")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.guName").value("종로구"))
                .andExpect(jsonPath("$.data.selectedAreaNm").value("광화문·덕수궁"))
                .andExpect(jsonPath("$.data.summary.weatherStatus").value("맑음"));
    }

    @Test
    @DisplayName("핫플레이스 offset 페이징 — 첫 페이지")
    void getDistrictHotplaces_firstPage() throws Exception {
        List<HotplaceRealtimeItem> all = IntStream.range(0, 10)
                .mapToObj(i -> HotplaceRealtimeItem.builder().areaNm("POI" + i).build())
                .toList();
        given(hotplaceRealtimeService.getDistrictHotplaces("종로구")).willReturn(all);

        mockMvc.perform(get("/realtime/districts/종로구/hotplaces")
                        .param("page", "0")
                        .param("size", "3")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(3))
                .andExpect(jsonPath("$.data.content[0].areaNm").value("POI0"))
                .andExpect(jsonPath("$.data.content[2].areaNm").value("POI2"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(3))
                .andExpect(jsonPath("$.data.totalElements").value(10))
                .andExpect(jsonPath("$.data.totalPages").value(4))
                .andExpect(jsonPath("$.data.first").value(true))
                .andExpect(jsonPath("$.data.last").value(false))
                .andExpect(jsonPath("$.data.hasNext").value(true))
                .andExpect(jsonPath("$.data.hasPrevious").value(false));
    }

    @Test
    @DisplayName("핫플레이스 offset 페이징 — 마지막 페이지는 last true, hasNext false")
    void getDistrictHotplaces_lastPage() throws Exception {
        List<HotplaceRealtimeItem> all = IntStream.range(0, 10)
                .mapToObj(i -> HotplaceRealtimeItem.builder().areaNm("POI" + i).build())
                .toList();
        given(hotplaceRealtimeService.getDistrictHotplaces("종로구")).willReturn(all);

        // 10개 ÷ 3 = 4페이지 (0,1,2,3). 마지막 페이지는 page=3 (POI9 한 개)
        mockMvc.perform(get("/realtime/districts/종로구/hotplaces")
                        .param("page", "3")
                        .param("size", "3")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].areaNm").value("POI9"))
                .andExpect(jsonPath("$.data.page").value(3))
                .andExpect(jsonPath("$.data.totalPages").value(4))
                .andExpect(jsonPath("$.data.first").value(false))
                .andExpect(jsonPath("$.data.last").value(true))
                .andExpect(jsonPath("$.data.hasNext").value(false))
                .andExpect(jsonPath("$.data.hasPrevious").value(true));
    }

    @Test
    @DisplayName("핫플레이스가 없으면 공통 에러 응답을 반환한다")
    void getDistrictSummary_returnsErrorResponse() throws Exception {
        given(hotplaceRealtimeService.getDistrictSummary("종로구"))
                .willThrow(new CustomException(ErrorCode.HOTPLACE_NOT_FOUND));

        mockMvc.perform(get("/realtime/districts/종로구/summary")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("HOTPLACE_NOT_FOUND"));
    }
}
