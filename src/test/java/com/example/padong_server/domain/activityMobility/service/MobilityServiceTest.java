package com.example.padong_server.domain.activityMobility.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.padong_server.domain.activityMobility.dto.CommonDepartureMobilityResponse;
import com.example.padong_server.domain.activityMobility.dto.MobilitySimpleResponse;
import com.example.padong_server.domain.activityMobility.entity.Mobility;
import com.example.padong_server.domain.activityMobility.repository.MobilityRepository;
import com.example.padong_server.domain.dongne.dto.AdminDongCsvRow;
import com.example.padong_server.domain.dongne.entity.AdminDong;
import com.example.padong_server.domain.dongne.service.DongneService;
import com.example.padong_server.domain.population.service.PopulationService;
import com.example.padong_server.domain.rentPrice.service.RentPriceService;
import com.example.padong_server.domain.score.service.ScoreCalculator;
import com.example.padong_server.global.ResponseDTO;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class MobilityServiceTest {

    @Mock
    private MobilityRepository mobilityRepository;

    @Mock
    private DongneService dongneService;

    @Mock
    private PopulationService populationService;

    @Mock
    private RentPriceService rentPriceService;

    @Mock
    private ScoreCalculator scoreCalculator;

    @InjectMocks
    private MobilityService mobilityService;

    @Test
    @DisplayName("행정동 코드 기준 단일 조회는 생활이동 많은 순 결과를 단순 응답으로 반환한다")
    void searchByArrivalDongCodeReturnsSimpleResponses() {
        AdminDong arrivalDong = adminDong("1168064000", "강남구", "역삼1동");
        AdminDong firstDepartureDong = adminDong("1162069500", "관악구", "신림동");
        AdminDong secondDepartureDong = adminDong("1121571000", "광진구", "화양동");
        Pageable pageable = PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "totalMobility"));
        List<Mobility> mobilities = List.of(
                mobility(arrivalDong, firstDepartureDong, 18432.274, 42.745),
                mobility(arrivalDong, secondDepartureDong, 15231.891, 38.444)
        );
        when(dongneService.findAdminDongByCode("1168064000")).thenReturn(arrivalDong);
        when(mobilityRepository.findByArrivalDong(arrivalDong, pageable))
                .thenReturn(new PageImpl<>(mobilities, pageable, mobilities.size()));

        ResponseDTO<List<MobilitySimpleResponse>> response =
                mobilityService.searchByArrivalDongCode("1168064000", pageable);

        assertThat(response.getStatusCode()).isEqualTo(String.valueOf(HttpStatus.OK.value()));
        assertThat(response.getMessage()).isEqualTo("생활이동 많은 순 조회 성공");
        assertThat(response.getData())
                .hasSize(2)
                .extracting(MobilitySimpleResponse::getTotalMobility, MobilitySimpleResponse::getAvgTime)
                .containsExactly(
                        tuple(18432.27, 42.75),
                        tuple(15231.89, 38.44)
                );
        assertThat(response.getData())
                .extracting(responseItem -> responseItem.getDepartureDong().getAdminDongCode())
                .containsExactly("1162069500", "1121571000");
        verifyNoInteractions(populationService, rentPriceService, scoreCalculator);
    }

    @Test
    @DisplayName("행정동 코드 기준 생활이동 데이터가 없으면 404 응답을 반환한다")
    void searchByArrivalDongCodeReturnsNotFoundWhenEmpty() {
        AdminDong arrivalDong = adminDong("1168064000", "강남구", "역삼1동");
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "totalMobility"));
        when(dongneService.findAdminDongByCode("1168064000")).thenReturn(arrivalDong);
        when(mobilityRepository.findByArrivalDong(arrivalDong, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        ResponseDTO<List<MobilitySimpleResponse>> response =
                mobilityService.searchByArrivalDongCode("1168064000", pageable);

        assertThat(response.getStatusCode()).isEqualTo(String.valueOf(HttpStatus.NOT_FOUND.value()));
        assertThat(response.getMessage()).isEqualTo("해당 생활이동 데이터가 존재하지 않습니다.");
        assertThat(response.getData()).isNull();
        verify(mobilityRepository).findByArrivalDong(arrivalDong, pageable);
        verifyNoInteractions(populationService, rentPriceService, scoreCalculator);
    }

    @Test
    @DisplayName("여러 행정동 코드 조회는 공통 출발동만 합산 생활이동 많은 순으로 반환한다")
    void searchByArrivalDongCodesReturnsCommonDepartureResponses() {
        AdminDong firstArrivalDong = adminDong("1168064000", "강남구", "역삼1동");
        AdminDong secondArrivalDong = adminDong("1156054000", "영등포구", "여의동");
        AdminDong firstCommonDepartureDong = adminDong("1162069500", "관악구", "신림동");
        AdminDong secondCommonDepartureDong = adminDong("1121571000", "광진구", "화양동");
        AdminDong notCommonDepartureDong = adminDong("1144066000", "마포구", "서교동");
        Pageable pageable = PageRequest.of(0, 10);
        List<Mobility> mobilities = List.of(
                mobility(firstArrivalDong, firstCommonDepartureDong, 100.123, 20.0),
                mobility(secondArrivalDong, firstCommonDepartureDong, 300.456, 40.0),
                mobility(firstArrivalDong, secondCommonDepartureDong, 120.0, 50.0),
                mobility(secondArrivalDong, secondCommonDepartureDong, 80.0, 30.0),
                mobility(firstArrivalDong, notCommonDepartureDong, 1000.0, 10.0)
        );
        when(dongneService.findAdminDongByCode("1168064000")).thenReturn(firstArrivalDong);
        when(dongneService.findAdminDongByCode("1156054000")).thenReturn(secondArrivalDong);
        when(mobilityRepository.findByArrivalDongIn(List.of(firstArrivalDong, secondArrivalDong)))
                .thenReturn(mobilities);

        ResponseDTO<List<CommonDepartureMobilityResponse>> response =
                mobilityService.searchByArrivalDongCodes(
                        List.of("1168064000", "1156054000"),
                        pageable
                );

        assertThat(response.getStatusCode()).isEqualTo(String.valueOf(HttpStatus.OK.value()));
        assertThat(response.getMessage()).isEqualTo("다중 행정동 생활이동 많은 순 조회 성공");
        assertThat(response.getData())
                .hasSize(2)
                .extracting(
                        responseItem -> responseItem.getDepartureDong().getAdminDongCode(),
                        CommonDepartureMobilityResponse::getTotalMobility
                )
                .containsExactly(
                        tuple("1162069500", 400.58),
                        tuple("1121571000", 200.0)
                );
        verifyNoInteractions(populationService, rentPriceService, scoreCalculator);
    }

    @Test
    @DisplayName("여러 행정동 코드 조회는 중복 제거 후 코드가 2개 미만이면 400 응답을 반환한다")
    void searchByArrivalDongCodesReturnsBadRequestWhenCodeCountIsLessThanTwo() {
        ResponseDTO<List<CommonDepartureMobilityResponse>> response =
                mobilityService.searchByArrivalDongCodes(List.of("1168064000", "1168064000"), PageRequest.of(0, 10));

        assertThat(response.getStatusCode()).isEqualTo(String.valueOf(HttpStatus.BAD_REQUEST.value()));
        assertThat(response.getMessage()).isEqualTo("행정동 코드는 2개 이상 입력해야 합니다.");
        assertThat(response.getData()).isNull();
        verifyNoInteractions(dongneService, mobilityRepository, populationService, rentPriceService, scoreCalculator);
    }

    @Test
    @DisplayName("여러 행정동 코드 조회는 공통 출발동이 없으면 404 응답을 반환한다")
    void searchByArrivalDongCodesReturnsNotFoundWhenCommonDepartureIsEmpty() {
        AdminDong firstArrivalDong = adminDong("1168064000", "강남구", "역삼1동");
        AdminDong secondArrivalDong = adminDong("1156054000", "영등포구", "여의동");
        AdminDong firstOnlyDepartureDong = adminDong("1162069500", "관악구", "신림동");
        AdminDong secondOnlyDepartureDong = adminDong("1121571000", "광진구", "화양동");
        Pageable pageable = PageRequest.of(0, 10);
        when(dongneService.findAdminDongByCode("1168064000")).thenReturn(firstArrivalDong);
        when(dongneService.findAdminDongByCode("1156054000")).thenReturn(secondArrivalDong);
        when(mobilityRepository.findByArrivalDongIn(List.of(firstArrivalDong, secondArrivalDong)))
                .thenReturn(List.of(
                        mobility(firstArrivalDong, firstOnlyDepartureDong, 100.0, 20.0),
                        mobility(secondArrivalDong, secondOnlyDepartureDong, 300.0, 40.0)
                ));

        ResponseDTO<List<CommonDepartureMobilityResponse>> response =
                mobilityService.searchByArrivalDongCodes(
                        List.of("1168064000", "1156054000"),
                        pageable
                );

        assertThat(response.getStatusCode()).isEqualTo(String.valueOf(HttpStatus.NOT_FOUND.value()));
        assertThat(response.getMessage()).isEqualTo("공통 생활이동 데이터가 존재하지 않습니다.");
        assertThat(response.getData()).isNull();
        verifyNoInteractions(populationService, rentPriceService, scoreCalculator);
    }

    private Mobility mobility(
            AdminDong arrivalDong,
            AdminDong departureDong,
            double totalMobility,
            double avgTime
    ) {
        return Mobility.builder()
                .startMonth("202601")
                .endMonth("202603")
                .arrivalDong(arrivalDong)
                .departureDong(departureDong)
                .totalMobility(totalMobility)
                .avgTime(avgTime)
                .build();
    }

    private AdminDong adminDong(String adminDongCode, String districtName, String adminDongName) {
        return new AdminDong(new AdminDongCsvRow(
                adminDongCode,
                "서울특별시",
                districtName,
                adminDongName,
                37.5,
                127.0,
                "20081101",
                ""
        ));
    }
}
