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
                .guName("\uC6A9\uC0B0\uAD6C")
                .selectedAreaNm("\uAD6D\uB9BD \uC911\uC559\uBC15\uBB3C\uAD00")
                .summary(WeatherSummary.builder()
                        .weatherStatus("\uB9D1\uC74C")
                        .temperature("21.3")
                        .pm10("\uC88B\uC74C")
                        .precipitationProbability("10%")
                        .build())
                .hotplaces(List.of(
                        HotplaceRealtimeItem.builder()
                                .category("\uBB38\uD654\uC720\uC0B0")
                                .areaNm("\uAD6D\uB9BD \uC911\uC559\uBC15\uBB3C\uAD00")
                                .congestionLevel("\uC5EC\uC720")
                                .congestionMessage("\uC0AC\uB78C\uC774 \uBAB0\uB824\uC788\uC744 \uAC00\uB2A5\uC131\uC774 \uB0AE\uACE0 \uBD90\uBE54\uC740 \uAC70\uC758 \uB290\uAEF4\uC9C0\uC9C0 \uC54A\uC544\uC694.")
                                .build()
                ))
                .build();

        given(hotplaceRealtimeService.getDistrictRealtime("\uC6A9\uC0B0\uAD6C")).willReturn(response);

        mockMvc.perform(get("/api/realtime/districts/{guName}", "\uC6A9\uC0B0\uAD6C")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.guName").value("\uC6A9\uC0B0\uAD6C"))
                .andExpect(jsonPath("$.selectedAreaNm").value("\uAD6D\uB9BD \uC911\uC559\uBC15\uBB3C\uAD00"))
                .andExpect(jsonPath("$.summary.weatherStatus").value("\uB9D1\uC74C"))
                .andExpect(jsonPath("$.summary.temperature").value("21.3"))
                .andExpect(jsonPath("$.summary.pm10").value("\uC88B\uC74C"))
                .andExpect(jsonPath("$.summary.precipitationProbability").value("10%"))
                .andExpect(jsonPath("$.hotplaces[0].category").value("\uBB38\uD654\uC720\uC0B0"))
                .andExpect(jsonPath("$.hotplaces[0].areaNm").value("\uAD6D\uB9BD \uC911\uC559\uBC15\uBB3C\uAD00"))
                .andExpect(jsonPath("$.hotplaces[0].congestionLevel").value("\uC5EC\uC720"))
                .andExpect(jsonPath("$.hotplaces[0].congestionMessage")
                        .value("\uC0AC\uB78C\uC774 \uBAB0\uB824\uC788\uC744 \uAC00\uB2A5\uC131\uC774 \uB0AE\uACE0 \uBD90\uBE54\uC740 \uAC70\uC758 \uB290\uAEF4\uC9C0\uC9C0 \uC54A\uC544\uC694."));
    }

    @Test
    @DisplayName("핫플레이스가 없으면 공통 에러 응답을 반환한다")
    void getDistrictRealtime_returnsErrorResponse() throws Exception {
        given(hotplaceRealtimeService.getDistrictRealtime("\uC6A9\uC0B0\uAD6C"))
                .willThrow(new CustomException(ErrorCode.HOTPLACE_NOT_FOUND));

        mockMvc.perform(get("/api/realtime/districts/{guName}", "\uC6A9\uC0B0\uAD6C")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("HOTPLACE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("\uD574\uB2F9 \uAD6C\uC5D0 \uB4F1\uB85D\uB41C \uD56B\uD50C\uB808\uC774\uC2A4\uAC00 \uC5C6\uC2B5\uB2C8\uB2E4."));
    }
}
