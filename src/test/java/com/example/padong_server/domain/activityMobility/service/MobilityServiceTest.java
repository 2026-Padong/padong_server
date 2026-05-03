package com.example.padong_server.domain.activityMobility.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.padong_server.domain.activityMobility.dto.CommonDepartureMobilityResponse;
import com.example.padong_server.domain.activityMobility.dto.MobilityFilterRequest;
import com.example.padong_server.domain.activityMobility.dto.MobilitySimpleResponse;
import com.example.padong_server.domain.activityMobility.entity.Mobility;
import com.example.padong_server.domain.activityMobility.repository.MobilityRepository;
import com.example.padong_server.domain.dongne.dto.AdminDongCsvRow;
import com.example.padong_server.domain.dongne.entity.AdminDong;
import com.example.padong_server.domain.dongne.service.DongneService;
import com.example.padong_server.domain.population.service.PopulationService;
import com.example.padong_server.domain.rentPrice.dto.request.RentPriceFilterCriteria;
import com.example.padong_server.domain.rentPrice.dto.response.AdminDongRentPriceBuildingTypeResponse;
import com.example.padong_server.domain.rentPrice.dto.response.AdminDongRentPriceDetailResponse;
import com.example.padong_server.domain.rentPrice.dto.response.MonthlyRentDisplayValueResponse;
import com.example.padong_server.domain.rentPrice.dto.response.RentPriceDisplayValueResponse;
import com.example.padong_server.domain.rentPrice.dto.response.ResidenceBuildingTypeResponse;
import com.example.padong_server.domain.rentPrice.dto.response.SelectedRentPriceResponse;
import com.example.padong_server.domain.rentPrice.entity.RentPriceTradeType;
import com.example.padong_server.domain.rentPrice.service.RentPriceService;
import com.example.padong_server.domain.score.service.ScoreCalculator;
import com.example.padong_server.global.PageResponse;
import com.example.padong_server.global.ResponseDTO;

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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
class MobilityServiceTest {

    @Mock private MobilityRepository mobilityRepository;

    @Mock private DongneService dongneService;

    @Mock private PopulationService populationService;

    @Mock private RentPriceService rentPriceService;

    @Mock private ScoreCalculator scoreCalculator;

    @InjectMocks private MobilityService mobilityService;

    @Test
    @DisplayName("행정동 코드 기준 단일 조회는 생활이동 많은 순 결과를 단순 응답으로 반환한다")
    void searchByArrivalDongCodeReturnsSimpleResponses() {
        AdminDong arrivalDong = adminDong("1168064000", "강남구", "역삼1동");
        AdminDong firstDepartureDong = adminDong("1162069500", "관악구", "신림동");
        AdminDong secondDepartureDong = adminDong("1121571000", "광진구", "화양동");
        Pageable pageable = PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "totalMobility"));
        List<Mobility> mobilities =
                List.of(
                        mobility(arrivalDong, firstDepartureDong, 18432.274, 42.745),
                        mobility(arrivalDong, secondDepartureDong, 15231.891, 38.444));
        when(dongneService.findAdminDongByCode("1168064000")).thenReturn(arrivalDong);
        when(mobilityRepository.findByArrivalDong(arrivalDong, pageable))
                .thenReturn(new PageImpl<>(mobilities, pageable, mobilities.size()));
        when(rentPriceService.getSelectedRentPrices(
                        List.of("1162069500", "1121571000"), any(RentPriceFilterCriteria.class)))
                .thenReturn(
                        selectedRentPriceMap(
                                defaultRentPriceDetail("1162069500", 500L, 45L),
                                defaultRentPriceDetail("1121571000", 700L, 55L)));

        ResponseDTO<PageResponse<MobilitySimpleResponse>> response =
                mobilityService.searchByArrivalDongCode("1168064000", pageable);

        assertThat(response.getStatusCode()).isEqualTo(String.valueOf(HttpStatus.OK.value()));
        assertThat(response.getMessage()).isEqualTo("생활이동 많은 순 조회 성공");
        assertThat(response.getData().content())
                .hasSize(2)
                .extracting(
                        MobilitySimpleResponse::getTotalMobility,
                        MobilitySimpleResponse::getAvgTime)
                .containsExactly(tuple(18432.27, 42.75), tuple(15231.89, 38.44));
        assertThat(response.getData().content())
                .extracting(responseItem -> responseItem.getDepartureDong().getAdminDongCode())
                .containsExactly("1162069500", "1121571000");
        assertThat(response.getData().page()).isZero();
        assertThat(response.getData().size()).isEqualTo(2);
        assertThat(response.getData().totalElements()).isEqualTo(2);
        assertThat(response.getData().totalPages()).isEqualTo(1);
        assertThat(response.getData().first()).isTrue();
        assertThat(response.getData().last()).isTrue();
        assertThat(response.getData().hasNext()).isFalse();
        assertThat(response.getData().hasPrevious()).isFalse();
        assertThat(
                        response.getData()
                                .content()
                                .get(0)
                                .getRentPrice()
                                .buildingType()
                                .buildingTypeCode())
                .isEqualTo("DETACHED_MULTIFAMILY");
        assertThat(response.getData().content().get(0).getRentPrice().tradeType().tradeTypeCode())
                .isEqualTo("MONTHLY_RENT");
        verifyNoInteractions(populationService, scoreCalculator);
    }

    @Test
    @DisplayName("행정동 코드 기준 생활이동 데이터가 없으면 404 응답을 반환한다")
    void searchByArrivalDongCodeReturnsNotFoundWhenEmpty() {
        AdminDong arrivalDong = adminDong("1168064000", "강남구", "역삼1동");
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "totalMobility"));
        when(dongneService.findAdminDongByCode("1168064000")).thenReturn(arrivalDong);
        when(mobilityRepository.findByArrivalDong(arrivalDong, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        ResponseDTO<PageResponse<MobilitySimpleResponse>> response =
                mobilityService.searchByArrivalDongCode("1168064000", pageable);

        assertThat(response.getStatusCode())
                .isEqualTo(String.valueOf(HttpStatus.NOT_FOUND.value()));
        assertThat(response.getMessage()).isEqualTo("해당 생활이동 데이터가 존재하지 않습니다.");
        assertThat(response.getData()).isNull();
        verify(mobilityRepository).findByArrivalDong(arrivalDong, pageable);
        verifyNoInteractions(populationService, rentPriceService, scoreCalculator);
    }

    @Test
    @DisplayName("행정동 코드 단일 조회 필터는 시간 범위와 자치구를 페이지네이션 전에 적용한다")
    void searchByArrivalDongCodeAppliesTimeAndDistrictFilters() {
        AdminDong arrivalDong = adminDong("1168064000", "강남구", "역삼1동");
        AdminDong matchingDepartureDong = adminDong("1162069500", "관악구", "신림동");
        AdminDong secondMatchingDepartureDong = adminDong("1162068500", "관악구", "봉천동");
        AdminDong timeExcludedDepartureDong = adminDong("1162071500", "관악구", "난곡동");
        AdminDong districtExcludedDepartureDong = adminDong("1159068000", "동작구", "상도1동");
        Pageable pageable = PageRequest.of(0, 1);
        MobilityFilterRequest filterRequest =
                MobilityFilterRequest.builder()
                        .minAvgTime(30.0)
                        .maxAvgTime(60.0)
                        .departureDistrictNames(List.of("관악구"))
                        .build();
        when(dongneService.findAdminDongByCode("1168064000")).thenReturn(arrivalDong);
        when(mobilityRepository.findByArrivalDong(arrivalDong))
                .thenReturn(
                        List.of(
                                mobility(arrivalDong, timeExcludedDepartureDong, 500.0, 70.0),
                                mobility(arrivalDong, districtExcludedDepartureDong, 400.0, 45.0),
                                mobility(arrivalDong, matchingDepartureDong, 300.0, 42.7),
                                mobility(arrivalDong, secondMatchingDepartureDong, 200.0, 40.0)));
        when(rentPriceService.getSelectedRentPrices(
                        List.of("1162069500"), any(RentPriceFilterCriteria.class)))
                .thenReturn(selectedRentPriceMap(defaultRentPriceDetail("1162069500", 500L, 45L)));

        ResponseDTO<PageResponse<MobilitySimpleResponse>> response =
                mobilityService.searchByArrivalDongCode("1168064000", pageable, filterRequest);

        assertThat(response.getStatusCode()).isEqualTo(String.valueOf(HttpStatus.OK.value()));
        assertThat(response.getData().content())
                .hasSize(1)
                .extracting(
                        responseItem -> responseItem.getDepartureDong().getAdminDongCode(),
                        MobilitySimpleResponse::getTotalMobility,
                        MobilitySimpleResponse::getAvgTime)
                .containsExactly(tuple("1162069500", 300.0, 42.7));
        assertThat(response.getData().page()).isZero();
        assertThat(response.getData().size()).isEqualTo(1);
        assertThat(response.getData().totalElements()).isEqualTo(2);
        assertThat(response.getData().totalPages()).isEqualTo(2);
        assertThat(response.getData().hasNext()).isTrue();
        assertThat(response.getData().content().get(0).getRentPrice().tradeType().tradeTypeCode())
                .isEqualTo("MONTHLY_RENT");
        verifyNoInteractions(populationService, scoreCalculator);
    }

    @Test
    @DisplayName("행정동 코드 단일 조회 필터는 월세 가격 범위를 만원 단위 요청값을 만원 단위 데이터와 비교해 적용한다")
    void searchByArrivalDongCodeAppliesMonthlyRentPriceRangeFilter() {
        AdminDong arrivalDong = adminDong("1168064000", "강남구", "역삼1동");
        AdminDong matchingDepartureDong = adminDong("1162069500", "관악구", "신림동");
        AdminDong excludedDepartureDong = adminDong("1121571000", "광진구", "화양동");
        Pageable pageable = PageRequest.of(0, 10);
        MobilityFilterRequest filterRequest =
                MobilityFilterRequest.builder()
                        .contractType("MONTHLY_RENT")
                        .houseType("OFFICETEL")
                        .minMonthlyDeposit(1_000L)
                        .maxMonthlyDeposit(5_000L)
                        .minMonthlyRent(50L)
                        .maxMonthlyRent(80L)
                        .build();
        when(dongneService.findAdminDongByCode("1168064000")).thenReturn(arrivalDong);
        when(mobilityRepository.findByArrivalDong(arrivalDong))
                .thenReturn(
                        List.of(
                                mobility(arrivalDong, excludedDepartureDong, 500.0, 40.0),
                                mobility(arrivalDong, matchingDepartureDong, 300.0, 42.7)));
        when(rentPriceService.findMatchedAdminDongCodes(
                        List.of("1121571000", "1162069500"), any(RentPriceFilterCriteria.class)))
                .thenReturn(Set.of("1162069500"));
        when(rentPriceService.getSelectedRentPrices(
                        List.of("1162069500"), any(RentPriceFilterCriteria.class)))
                .thenReturn(
                        selectedRentPriceMap(
                                rentPriceDetail(
                                        "1162069500",
                                        "OFFICETEL",
                                        null,
                                        null,
                                        monthlyRent(3_000L, 70L))));

        ResponseDTO<PageResponse<MobilitySimpleResponse>> response =
                mobilityService.searchByArrivalDongCode("1168064000", pageable, filterRequest);

        assertThat(response.getStatusCode()).isEqualTo(String.valueOf(HttpStatus.OK.value()));
        assertThat(response.getData().content())
                .hasSize(1)
                .extracting(responseItem -> responseItem.getDepartureDong().getAdminDongCode())
                .containsExactly("1162069500");
        SelectedRentPriceResponse rentPrice = response.getData().content().get(0).getRentPrice();
        assertThat(rentPrice.buildingType().buildingTypeCode()).isEqualTo("OFFICETEL");
        assertThat(rentPrice.tradeType().tradeTypeCode()).isEqualTo("MONTHLY_RENT");
        assertThat(rentPrice.price())
                .isInstanceOfSatisfying(
                        MonthlyRentDisplayValueResponse.class,
                        price -> {
                            assertThat(price.deposit()).isEqualTo(3_000L);
                            assertThat(price.monthlyRent()).isEqualTo(70L);
                        });
        assertThat(response.getData().totalElements()).isEqualTo(1);
        assertThat(response.getData().totalPages()).isEqualTo(1);
        verifyNoInteractions(populationService, scoreCalculator);
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
        List<Mobility> mobilities =
                List.of(
                        mobility(firstArrivalDong, firstCommonDepartureDong, 100.123, 20.0),
                        mobility(secondArrivalDong, firstCommonDepartureDong, 300.456, 40.0),
                        mobility(firstArrivalDong, secondCommonDepartureDong, 120.0, 50.0),
                        mobility(secondArrivalDong, secondCommonDepartureDong, 80.0, 30.0),
                        mobility(firstArrivalDong, notCommonDepartureDong, 1000.0, 10.0));
        when(dongneService.findAdminDongByCode("1168064000")).thenReturn(firstArrivalDong);
        when(dongneService.findAdminDongByCode("1156054000")).thenReturn(secondArrivalDong);
        when(mobilityRepository.findByArrivalDongIn(List.of(firstArrivalDong, secondArrivalDong)))
                .thenReturn(mobilities);
        when(rentPriceService.getSelectedRentPrices(
                        List.of("1162069500", "1121571000"), any(RentPriceFilterCriteria.class)))
                .thenReturn(
                        selectedRentPriceMap(
                                defaultRentPriceDetail("1162069500", 500L, 45L),
                                defaultRentPriceDetail("1121571000", 700L, 55L)));

        ResponseDTO<PageResponse<CommonDepartureMobilityResponse>> response =
                mobilityService.searchByArrivalDongCodes(
                        List.of("1168064000", "1156054000"), pageable);

        assertThat(response.getStatusCode()).isEqualTo(String.valueOf(HttpStatus.OK.value()));
        assertThat(response.getMessage()).isEqualTo("다중 행정동 생활이동 많은 순 조회 성공");
        assertThat(response.getData().content())
                .hasSize(2)
                .extracting(
                        responseItem -> responseItem.getDepartureDong().getAdminDongCode(),
                        CommonDepartureMobilityResponse::getTotalMobility)
                .containsExactly(tuple("1162069500", 400.58), tuple("1121571000", 200.0));
        assertThat(response.getData().page()).isZero();
        assertThat(response.getData().size()).isEqualTo(10);
        assertThat(response.getData().totalElements()).isEqualTo(2);
        assertThat(response.getData().totalPages()).isEqualTo(1);
        assertThat(
                        response.getData()
                                .content()
                                .get(0)
                                .getRentPrice()
                                .buildingType()
                                .buildingTypeCode())
                .isEqualTo("DETACHED_MULTIFAMILY");
        assertThat(response.getData().content().get(0).getRentPrice().tradeType().tradeTypeCode())
                .isEqualTo("MONTHLY_RENT");
        verifyNoInteractions(populationService, scoreCalculator);
    }

    @Test
    @DisplayName("여러 행정동 코드 조회 필터는 모든 OD의 시간 범위와 자치구를 만족하는 공통 출발동만 반환한다")
    void searchByArrivalDongCodesAppliesEachTimeAndDistrictFilters() {
        AdminDong firstArrivalDong = adminDong("1168064000", "강남구", "역삼1동");
        AdminDong secondArrivalDong = adminDong("1156054000", "영등포구", "여의동");
        AdminDong matchingDepartureDong = adminDong("1162069500", "관악구", "신림동");
        AdminDong timeExcludedDepartureDong = adminDong("1162071500", "관악구", "난곡동");
        AdminDong districtExcludedDepartureDong = adminDong("1121571000", "광진구", "화양동");
        Pageable pageable = PageRequest.of(0, 10);
        MobilityFilterRequest filterRequest =
                MobilityFilterRequest.builder()
                        .minEachAvgTime(30.0)
                        .maxEachAvgTime(60.0)
                        .departureDistrictNames(List.of("관악구"))
                        .build();
        when(dongneService.findAdminDongByCode("1168064000")).thenReturn(firstArrivalDong);
        when(dongneService.findAdminDongByCode("1156054000")).thenReturn(secondArrivalDong);
        when(mobilityRepository.findByArrivalDongIn(List.of(firstArrivalDong, secondArrivalDong)))
                .thenReturn(
                        List.of(
                                mobility(firstArrivalDong, matchingDepartureDong, 100.0, 40.0),
                                mobility(secondArrivalDong, matchingDepartureDong, 300.0, 50.0),
                                mobility(firstArrivalDong, timeExcludedDepartureDong, 120.0, 40.0),
                                mobility(secondArrivalDong, timeExcludedDepartureDong, 80.0, 70.0),
                                mobility(
                                        firstArrivalDong,
                                        districtExcludedDepartureDong,
                                        500.0,
                                        40.0),
                                mobility(
                                        secondArrivalDong,
                                        districtExcludedDepartureDong,
                                        500.0,
                                        40.0)));
        when(rentPriceService.getSelectedRentPrices(
                        List.of("1162069500"), any(RentPriceFilterCriteria.class)))
                .thenReturn(selectedRentPriceMap(defaultRentPriceDetail("1162069500", 500L, 45L)));

        ResponseDTO<PageResponse<CommonDepartureMobilityResponse>> response =
                mobilityService.searchByArrivalDongCodes(
                        List.of("1168064000", "1156054000"), pageable, filterRequest);

        assertThat(response.getStatusCode()).isEqualTo(String.valueOf(HttpStatus.OK.value()));
        assertThat(response.getData().content())
                .hasSize(1)
                .extracting(
                        responseItem -> responseItem.getDepartureDong().getAdminDongCode(),
                        CommonDepartureMobilityResponse::getTotalMobility)
                .containsExactly(tuple("1162069500", 400.0));
        assertThat(response.getData().totalElements()).isEqualTo(1);
        assertThat(response.getData().totalPages()).isEqualTo(1);
        assertThat(response.getData().content().get(0).getRentPrice().tradeType().tradeTypeCode())
                .isEqualTo("MONTHLY_RENT");
        verifyNoInteractions(populationService, scoreCalculator);
    }

    @Test
    @DisplayName("여러 행정동 코드 조회 가격 필터는 선택 주거 형태와 가격 정보를 함께 반환한다")
    void searchByArrivalDongCodesReturnsSelectedRentPrice() {
        AdminDong firstArrivalDong = adminDong("1168064000", "강남구", "역삼1동");
        AdminDong secondArrivalDong = adminDong("1156054000", "영등포구", "여의동");
        AdminDong matchingDepartureDong = adminDong("1162069500", "관악구", "신림동");
        Pageable pageable = PageRequest.of(0, 10);
        MobilityFilterRequest filterRequest =
                MobilityFilterRequest.builder()
                        .contractType("MONTHLY_RENT")
                        .houseType("OFFICETEL")
                        .minMonthlyDeposit(1_000L)
                        .maxMonthlyDeposit(5_000L)
                        .minMonthlyRent(50L)
                        .maxMonthlyRent(80L)
                        .build();
        when(dongneService.findAdminDongByCode("1168064000")).thenReturn(firstArrivalDong);
        when(dongneService.findAdminDongByCode("1156054000")).thenReturn(secondArrivalDong);
        when(mobilityRepository.findByArrivalDongIn(List.of(firstArrivalDong, secondArrivalDong)))
                .thenReturn(
                        List.of(
                                mobility(firstArrivalDong, matchingDepartureDong, 100.0, 40.0),
                                mobility(secondArrivalDong, matchingDepartureDong, 300.0, 50.0)));
        when(rentPriceService.findMatchedAdminDongCodes(
                        List.of("1162069500"), any(RentPriceFilterCriteria.class)))
                .thenReturn(Set.of("1162069500"));
        when(rentPriceService.getSelectedRentPrices(
                        List.of("1162069500"), any(RentPriceFilterCriteria.class)))
                .thenReturn(
                        selectedRentPriceMap(
                                rentPriceDetail(
                                        "1162069500",
                                        "OFFICETEL",
                                        null,
                                        null,
                                        monthlyRent(3_000L, 70L))));

        ResponseDTO<PageResponse<CommonDepartureMobilityResponse>> response =
                mobilityService.searchByArrivalDongCodes(
                        List.of("1168064000", "1156054000"), pageable, filterRequest);

        assertThat(response.getStatusCode()).isEqualTo(String.valueOf(HttpStatus.OK.value()));
        CommonDepartureMobilityResponse responseItem = response.getData().content().get(0);
        assertThat(responseItem.getDepartureDong().getAdminDongCode()).isEqualTo("1162069500");
        assertThat(responseItem.getTotalMobility()).isEqualTo(400.0);
        assertThat(responseItem.getRentPrice().buildingType().buildingTypeCode())
                .isEqualTo("OFFICETEL");
        assertThat(responseItem.getRentPrice().tradeType().tradeTypeCode())
                .isEqualTo("MONTHLY_RENT");
        assertThat(responseItem.getRentPrice().price())
                .isInstanceOfSatisfying(
                        MonthlyRentDisplayValueResponse.class,
                        price -> {
                            assertThat(price.deposit()).isEqualTo(3_000L);
                            assertThat(price.monthlyRent()).isEqualTo(70L);
                        });
        verifyNoInteractions(populationService, scoreCalculator);
    }

    @Test
    @DisplayName("가격 필터와 거래 형태가 맞지 않으면 필터 검증이 실패한다")
    void filterRequestIsInvalidWhenPriceFilterMismatchesContractType() {
        MobilityFilterRequest filterRequest =
                MobilityFilterRequest.builder()
                        .contractType("SALE")
                        .minJeonseDeposit(10_000L)
                        .build();

        assertThat(filterRequest.isPriceFilterValid()).isFalse();
        verifyNoInteractions(
                dongneService,
                mobilityRepository,
                populationService,
                rentPriceService,
                scoreCalculator);
    }

    @Test
    @DisplayName("여러 행정동 코드 조회는 중복 제거 후 코드가 2개 미만이면 400 응답을 반환한다")
    void searchByArrivalDongCodesReturnsBadRequestWhenCodeCountIsLessThanTwo() {
        ResponseDTO<PageResponse<CommonDepartureMobilityResponse>> response =
                mobilityService.searchByArrivalDongCodes(
                        List.of("1168064000", "1168064000"), PageRequest.of(0, 10));

        assertThat(response.getStatusCode())
                .isEqualTo(String.valueOf(HttpStatus.BAD_REQUEST.value()));
        assertThat(response.getMessage()).isEqualTo("행정동 코드는 2개 이상 입력해야 합니다.");
        assertThat(response.getData()).isNull();
        verifyNoInteractions(
                dongneService,
                mobilityRepository,
                populationService,
                rentPriceService,
                scoreCalculator);
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
                .thenReturn(
                        List.of(
                                mobility(firstArrivalDong, firstOnlyDepartureDong, 100.0, 20.0),
                                mobility(secondArrivalDong, secondOnlyDepartureDong, 300.0, 40.0)));

        ResponseDTO<PageResponse<CommonDepartureMobilityResponse>> response =
                mobilityService.searchByArrivalDongCodes(
                        List.of("1168064000", "1156054000"), pageable);

        assertThat(response.getStatusCode())
                .isEqualTo(String.valueOf(HttpStatus.NOT_FOUND.value()));
        assertThat(response.getMessage()).isEqualTo("공통 생활이동 데이터가 존재하지 않습니다.");
        assertThat(response.getData()).isNull();
        verifyNoInteractions(populationService, rentPriceService, scoreCalculator);
    }

    private Mobility mobility(
            AdminDong arrivalDong, AdminDong departureDong, double totalMobility, double avgTime) {
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
        return new AdminDong(
                new AdminDongCsvRow(
                        adminDongCode,
                        "서울특별시",
                        districtName,
                        adminDongName,
                        37.5,
                        127.0,
                        "20081101",
                        ""));
    }

    private AdminDongRentPriceDetailResponse defaultRentPriceDetail(
            String adminDongCode, Long deposit, Long monthlyRent) {
        return rentPriceDetail(
                adminDongCode,
                "DETACHED_MULTIFAMILY",
                null,
                null,
                monthlyRent(deposit, monthlyRent));
    }

    private Map<String, SelectedRentPriceResponse> selectedRentPriceMap(
            AdminDongRentPriceDetailResponse... details) {
        Map<String, SelectedRentPriceResponse> selectedRentPriceByAdminDongCode =
                new LinkedHashMap<>();
        for (AdminDongRentPriceDetailResponse detail : details) {
            selectedRentPriceByAdminDongCode.put(
                    detail.adminDongCode(),
                    SelectedRentPriceResponse.from(
                            detail.buildingTypes().get(0), RentPriceTradeType.MONTHLY_RENT));
        }
        return selectedRentPriceByAdminDongCode;
    }

    private AdminDongRentPriceDetailResponse rentPriceDetail(
            String adminDongCode,
            String houseType,
            RentPriceDisplayValueResponse sale,
            RentPriceDisplayValueResponse jeonse,
            MonthlyRentDisplayValueResponse monthlyRent) {
        return new AdminDongRentPriceDetailResponse(
                adminDongCode,
                "최근 2년 기준",
                "2024-04-18",
                "2026-04-17",
                true,
                null,
                List.of(
                        new AdminDongRentPriceBuildingTypeResponse(
                                new ResidenceBuildingTypeResponse(houseType, houseType),
                                sale == null ? new RentPriceDisplayValueResponse(null) : sale,
                                jeonse == null ? new RentPriceDisplayValueResponse(null) : jeonse,
                                monthlyRent == null
                                        ? new MonthlyRentDisplayValueResponse(null, null)
                                        : monthlyRent)));
    }

    private MonthlyRentDisplayValueResponse monthlyRent(Long deposit, Long monthlyRent) {
        return new MonthlyRentDisplayValueResponse(deposit, monthlyRent);
    }
}
