package com.example.padong_server.domain.activityMobility.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.padong_server.domain.activityMobility.service.MobilityImportService;
import com.example.padong_server.domain.activityMobility.service.MobilityService;
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

@ExtendWith(MockitoExtension.class)
class MobilityControllerTest {

    @Mock
    private MobilityService mobilityService;

    @Mock
    private MobilityImportService mobilityImportService;

    @InjectMocks
    private MobilityController mobilityController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(mobilityController).build();
    }

    @Test
    @DisplayName("생활이동 데이터 적재 endpoint는 완료 안내 문구를 공통 응답으로 반환한다")
    void fetchDataReturnsImportMessage() throws Exception {
        String message = "생활이동 데이터 적재 완료: 202601~202603 기준, 142939건 저장";
        given(mobilityImportService.importData()).willReturn(message);

        mockMvc.perform(post("/mobility/data")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("200"))
                .andExpect(jsonPath("$.message").value(message));

        verify(mobilityImportService).importData();
    }
}
