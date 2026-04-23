package com.example.padong_server.domain.subway.controller;

import com.example.padong_server.domain.subway.util.SubwayCsvLoader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SubwayController.class)
class SubwayControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SubwayCsvLoader subwayCsvLoader;

    @Test
    @DisplayName("CSV 적재 API 성공 테스트")
    void loadCsv_success() throws Exception {

        mockMvc.perform(get("/subway/load"))
                .andExpect(status().isOk())
                .andExpect(content().string("CSV 데이터 적재 완료"));

        verify(subwayCsvLoader, times(1)).loadCsv();
    }
}