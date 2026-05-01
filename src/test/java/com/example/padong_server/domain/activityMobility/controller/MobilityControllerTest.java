package com.example.padong_server.domain.activityMobility.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.padong_server.domain.activityMobility.dto.CommonDepartureMobilityResponse;
import com.example.padong_server.domain.activityMobility.dto.MobilityFilterRequest;
import com.example.padong_server.domain.activityMobility.dto.MobilitySimpleResponse;
import com.example.padong_server.domain.activityMobility.service.MobilityImportService;
import com.example.padong_server.domain.activityMobility.service.MobilityService;
import com.example.padong_server.domain.dongne.dto.AdminDongDto;
import com.example.padong_server.global.ResponseDTO;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
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

    @Test
    @DisplayName("행정동 코드 단일 조회 endpoint는 생활이동 많은 순 단순 응답을 반환한다")
    void searchByArrivalDongCodeReturnsSimpleResponses() throws Exception {
        MobilitySimpleResponse responseItem = MobilitySimpleResponse.builder()
                .departureDong(AdminDongDto.builder()
                        .adminDongCode("1162069500")
                        .address("서울특별시 관악구 신림동")
                        .build())
                .totalMobility(18432.27)
                .avgTime(42.7)
                .build();
        given(mobilityService.searchByArrivalDongCode(
                        eq("1168064000"),
                        any(Pageable.class),
                        any(MobilityFilterRequest.class)
                ))
                .willReturn(ResponseDTO.res(HttpStatus.OK, "생활이동 많은 순 조회 성공", List.of(responseItem)));

        mockMvc.perform(get("/mobility/arrival/{adminDongCode}", "1168064000")
                        .param("page", "1")
                        .param("size", "5")
                        .param("minAvgTime", "30")
                        .param("maxAvgTime", "60")
                        .param("departureDistrictNames", "관악구")
                        .param("contractType", "MONTHLY_RENT")
                        .param("houseType", "OFFICETEL")
                        .param("minMonthlyDeposit", "1000")
                        .param("maxMonthlyDeposit", "5000")
                        .param("minMonthlyRent", "50")
                        .param("maxMonthlyRent", "80")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("200"))
                .andExpect(jsonPath("$.message").value("생활이동 많은 순 조회 성공"))
                .andExpect(jsonPath("$.data[0].departureDong.adminDongCode").value("1162069500"))
                .andExpect(jsonPath("$.data[0].departureDong.address").value("서울특별시 관악구 신림동"))
                .andExpect(jsonPath("$.data[0].totalMobility").value(18432.27))
                .andExpect(jsonPath("$.data[0].avgTime").value(42.7));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        ArgumentCaptor<MobilityFilterRequest> filterCaptor =
                ArgumentCaptor.forClass(MobilityFilterRequest.class);
        verify(mobilityService).searchByArrivalDongCode(
                eq("1168064000"),
                pageableCaptor.capture(),
                filterCaptor.capture()
        );
        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(1);
        assertThat(pageable.getPageSize()).isEqualTo(5);
        assertThat(pageable.getSort().getOrderFor("totalMobility"))
                .extracting(Sort.Order::getDirection)
                .isEqualTo(Sort.Direction.DESC);
        MobilityFilterRequest filterRequest = filterCaptor.getValue();
        assertThat(filterRequest.getMinAvgTime()).isEqualTo(30.0);
        assertThat(filterRequest.getMaxAvgTime()).isEqualTo(60.0);
        assertThat(filterRequest.getDepartureDistrictNames()).containsExactly("관악구");
        assertThat(filterRequest.getContractType()).isEqualTo("MONTHLY_RENT");
        assertThat(filterRequest.getHouseType()).isEqualTo("OFFICETEL");
        assertThat(filterRequest.getMinMonthlyDeposit()).isEqualTo(1_000L);
        assertThat(filterRequest.getMaxMonthlyDeposit()).isEqualTo(5_000L);
        assertThat(filterRequest.getMinMonthlyRent()).isEqualTo(50L);
        assertThat(filterRequest.getMaxMonthlyRent()).isEqualTo(80L);
    }

    @Test
    @DisplayName("여러 행정동 코드 조회 endpoint는 반복 쿼리 파라미터를 리스트로 받아 공통 결과를 반환한다")
    void searchByArrivalDongCodesReturnsCommonSimpleResponses() throws Exception {
        CommonDepartureMobilityResponse responseItem = CommonDepartureMobilityResponse.builder()
                .departureDong(AdminDongDto.builder()
                        .adminDongCode("1162069500")
                        .address("서울특별시 관악구 신림동")
                        .build())
                .totalMobility(400.0)
                .build();
        given(mobilityService.searchByArrivalDongCodes(
                        eq(List.of("1168064000", "1156054000")),
                        any(Pageable.class),
                        any(MobilityFilterRequest.class)
                ))
                .willReturn(ResponseDTO.res(
                        HttpStatus.OK,
                        "다중 행정동 생활이동 많은 순 조회 성공",
                        List.of(responseItem)
                ));

        mockMvc.perform(get("/mobility/arrival/multi")
                        .param("arrivalDongCodes", "1168064000")
                        .param("arrivalDongCodes", "1156054000")
                        .param("page", "1")
                        .param("size", "5")
                        .param("minEachAvgTime", "0")
                        .param("maxEachAvgTime", "60")
                        .param("departureDistrictNames", "관악구")
                        .param("contractType", "JEONSE")
                        .param("houseType", "APARTMENT")
                        .param("minJeonseDeposit", "10000")
                        .param("maxJeonseDeposit", "30000")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("200"))
                .andExpect(jsonPath("$.message").value("다중 행정동 생활이동 많은 순 조회 성공"))
                .andExpect(jsonPath("$.data[0].departureDong.adminDongCode").value("1162069500"))
                .andExpect(jsonPath("$.data[0].departureDong.address").value("서울특별시 관악구 신림동"))
                .andExpect(jsonPath("$.data[0].totalMobility").value(400.0))
                .andExpect(jsonPath("$.data[0].avgTime").doesNotExist());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        ArgumentCaptor<MobilityFilterRequest> filterCaptor =
                ArgumentCaptor.forClass(MobilityFilterRequest.class);
        verify(mobilityService).searchByArrivalDongCodes(
                eq(List.of("1168064000", "1156054000")),
                pageableCaptor.capture(),
                filterCaptor.capture()
        );
        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(1);
        assertThat(pageable.getPageSize()).isEqualTo(5);
        assertThat(pageable.getSort().isUnsorted()).isTrue();
        MobilityFilterRequest filterRequest = filterCaptor.getValue();
        assertThat(filterRequest.getMinEachAvgTime()).isEqualTo(0.0);
        assertThat(filterRequest.getMaxEachAvgTime()).isEqualTo(60.0);
        assertThat(filterRequest.getDepartureDistrictNames()).containsExactly("관악구");
        assertThat(filterRequest.getContractType()).isEqualTo("JEONSE");
        assertThat(filterRequest.getHouseType()).isEqualTo("APARTMENT");
        assertThat(filterRequest.getMinJeonseDeposit()).isEqualTo(10_000L);
        assertThat(filterRequest.getMaxJeonseDeposit()).isEqualTo(30_000L);
    }
}
