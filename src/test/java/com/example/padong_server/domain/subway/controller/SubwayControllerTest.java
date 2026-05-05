package com.example.padong_server.domain.subway.controller;

import com.example.padong_server.domain.subway.util.SubwayCsvLoader;
import com.example.padong_server.domain.subway.util.SubwayTransferCsvLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SubwayControllerTest {

    @Mock
    private SubwayCsvLoader subwayCsvLoader;

    @Mock
    private SubwayTransferCsvLoader subwayTransferCsvLoader;

    @InjectMocks
    private SubwayController subwayController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(subwayController).build();
    }

    @Test
    @DisplayName("CSV 적재 API 호출 시 성공 응답을 반환한다")
    void loadCsv_success() throws Exception {
        mockMvc.perform(get("/subway/load"))
                .andExpect(status().isOk())
                .andExpect(content().string("CSV 데이터 적재 완료"));

        verify(subwayCsvLoader, times(1)).loadCsv();
    }
}
