package com.example.padong_server.domain.rentPrice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.example.padong_server.domain.dongne.dto.AdminDongCsvRow;
import com.example.padong_server.domain.dongne.entity.AdminDong;
import com.example.padong_server.domain.dongne.repository.AdminDongRepository;
import com.example.padong_server.domain.rentPrice.dto.response.AdminDongRentPriceDetailResponse;
import com.example.padong_server.domain.rentPrice.dto.response.AdminDongRentPriceSummaryResponse;
import com.example.padong_server.domain.rentPrice.entity.RentPrice;
import com.example.padong_server.domain.rentPrice.policy.RentPriceDisplayPolicy;
import com.example.padong_server.domain.rentPrice.repository.RentPriceRepository;
import com.example.padong_server.global.exception.CustomException;
import com.example.padong_server.global.exception.ErrorCode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class RentPriceServiceTest {

    @Mock private AdminDongRepository adminDongRepository;

    @Mock private RentPriceRepository rentPriceRepository;

    private RentPriceService rentPriceService;

    @BeforeEach
    void setUp() {
        rentPriceService =
                new RentPriceService(
                        adminDongRepository, rentPriceRepository, new RentPriceDisplayPolicy());
    }

    @Test
    @DisplayName("배치 요약은 요청 순서를 유지하고 선택한 필터 기준 대표값을 반환한다")
    void returnsBatchSummariesInRequestedOrder() {
        AdminDong first = adminDong("1111051500", "청운효자동");
        AdminDong second = adminDong("1111053000", "사직동");
        when(adminDongRepository.findAllByAdminDongCodeIn(List.of("1111053000", "1111051500")))
                .thenReturn(List.of(first, second));
        when(rentPriceRepository.findAllByAdminDongAdminDongCodeIn(
                        List.of("1111053000", "1111051500")))
                .thenReturn(
                        List.of(
                                adminStat(first, "아파트", 98_000L, 5, 50_000L, 8, 1_000L, 85L, 12),
                                adminStat(second, "오피스텔", 70_000L, 0, 35_000L, 4, 500L, 45L, 3),
                                adminStat(
                                        second, "아파트", 90_000L, 31, 45_000L, 35, 1_200L, 70L, 30)));

        List<AdminDongRentPriceSummaryResponse> responses =
                rentPriceService.getSummaries(List.of("1111053000", "1111051500"), "아파트", "매매");

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).adminDongCode()).isEqualTo("1111053000");
        assertThat(responses.get(0).buildingType().buildingTypeCode()).isEqualTo("APARTMENT");
        assertThat(responses.get(0).tradeType().tradeTypeCode()).isEqualTo("SALE");
        assertThat(responses.get(0).sale().amount()).isEqualTo(90_000L);

        assertThat(responses.get(1).adminDongCode()).isEqualTo("1111051500");
        assertThat(responses.get(1).buildingType().buildingTypeCode()).isEqualTo("APARTMENT");
        assertThat(responses.get(1).sale().amount()).isEqualTo(98_000L);
        assertThat(responses.get(1).jeonse().amount()).isNull();
        assertThat(responses.get(1).monthlyRent().deposit()).isNull();
        assertThat(responses.get(1).monthlyRent().monthlyRent()).isNull();
    }

    @Test
    @DisplayName("필터 요약은 선택한 건물유형과 거래유형 가격만 반환한다")
    void returnsFilteredSummaryByBuildingTypeAndTradeType() {
        AdminDong adminDong = adminDong("1111051500", "청운효자동");
        when(adminDongRepository.findAllByAdminDongCodeIn(List.of("1111051500")))
                .thenReturn(List.of(adminDong));
        when(rentPriceRepository.findAllByAdminDongAdminDongCodeIn(List.of("1111051500")))
                .thenReturn(
                        List.of(
                                adminStat(
                                        adminDong, "아파트", 98_000L, 12, 50_000L, 8, 1_000L, 85L, 12),
                                adminStat(
                                        adminDong, "단독다가구", 55_000L, 2, 24_000L, 7, 500L, 45L,
                                        30)));

        List<AdminDongRentPriceSummaryResponse> responses =
                rentPriceService.getSummaries(List.of("1111051500"), "단독다가구", "월세");

        assertThat(responses).hasSize(1);
        AdminDongRentPriceSummaryResponse response = responses.get(0);
        assertThat(response.adminDongCode()).isEqualTo("1111051500");
        assertThat(response.buildingType().buildingTypeCode()).isEqualTo("DETACHED_MULTIFAMILY");
        assertThat(response.tradeType().tradeTypeCode()).isEqualTo("MONTHLY_RENT");
        assertThat(response.tradeType().tradeTypeLabel()).isEqualTo("월세");
        assertThat(response.sale().amount()).isNull();
        assertThat(response.jeonse().amount()).isNull();
        assertThat(response.monthlyRent().deposit()).isEqualTo(500L);
        assertThat(response.monthlyRent().monthlyRent()).isEqualTo(45L);
    }

    @Test
    @DisplayName("필터 요약 기본값은 단독다가구 월세다")
    void usesDetachedMultifamilyMonthlyRentAsDefaultSummaryFilter() {
        AdminDong adminDong = adminDong("1111051500", "청운효자동");
        when(adminDongRepository.findAllByAdminDongCodeIn(List.of("1111051500")))
                .thenReturn(List.of(adminDong));
        when(rentPriceRepository.findAllByAdminDongAdminDongCodeIn(List.of("1111051500")))
                .thenReturn(
                        List.of(
                                adminStat(
                                        adminDong, "아파트", 98_000L, 12, 50_000L, 8, 1_000L, 85L, 12),
                                adminStat(
                                        adminDong, "단독다가구", 55_000L, 2, 24_000L, 7, 500L, 45L,
                                        30)));

        List<AdminDongRentPriceSummaryResponse> responses =
                rentPriceService.getSummaries(List.of("1111051500"), null, null);

        assertThat(responses).hasSize(1);
        AdminDongRentPriceSummaryResponse response = responses.get(0);
        assertThat(response.buildingType().buildingTypeCode()).isEqualTo("DETACHED_MULTIFAMILY");
        assertThat(response.tradeType().tradeTypeCode()).isEqualTo("MONTHLY_RENT");
        assertThat(response.monthlyRent().deposit()).isEqualTo(500L);
        assertThat(response.monthlyRent().monthlyRent()).isEqualTo(45L);
    }

    @Test
    @DisplayName("상세 조회는 4개 건물유형을 모두 반환하고 없는 유형은 NO_DATA로 채운다")
    void returnsDetailWithAllBuildingTypes() {
        AdminDong adminDong = adminDong("1111051500", "청운효자동");
        doReturn(adminDong).when(adminDongRepository).getByAdminDongCode("1111051500");
        when(rentPriceRepository.findAllByAdminDongAdminDongCode("1111051500"))
                .thenReturn(
                        List.of(
                                adminStat(
                                        adminDong, "아파트", 98_000L, 12, 50_000L, 8, 1_000L, 85L, 12),
                                adminStat(
                                        adminDong, "오피스텔", 70_000L, 3, 35_000L, 2, 500L, 45L, 3)));

        AdminDongRentPriceDetailResponse response = rentPriceService.getDetail("1111051500");

        assertThat(response.adminDongCode()).isEqualTo("1111051500");
        assertThat(response.periodLabel()).isEqualTo("최근 2년 기준");
        assertThat(response.contractPeriodStart()).isEqualTo("2024-04-18");
        assertThat(response.contractPeriodEnd()).isEqualTo("2026-04-17");
        assertThat(response.excludedCancelledSales()).isTrue();
        assertThat(response.dominantBuildingType().buildingTypeCode()).isEqualTo("APARTMENT");
        assertThat(response.buildingTypes()).hasSize(4);
        assertThat(response.buildingTypes())
                .extracting(item -> item.buildingType().buildingTypeCode())
                .containsExactly(
                        "APARTMENT", "OFFICETEL", "ROW_MULTIFAMILY", "DETACHED_MULTIFAMILY");
        assertThat(response.buildingTypes().get(0).sale().amount()).isEqualTo(98_000L);
        assertThat(response.buildingTypes().get(1).sale().amount()).isEqualTo(70_000L);
        assertThat(response.buildingTypes().get(2).sale().amount()).isNull();
    }

    @Test
    @DisplayName("존재하지 않는 행정동 코드는 bad request 대상 예외를 낸다")
    void failsWhenAdminDongCodeIsUnknown() {
        doThrow(new CustomException(ErrorCode.VALIDATION_ERROR, "존재하지 않는 행정동 코드입니다: 9999999999"))
                .when(adminDongRepository)
                .getByAdminDongCode("9999999999");

        assertThatThrownBy(() -> rentPriceService.getDetail("9999999999"))
                .isInstanceOf(CustomException.class)
                .hasMessage("존재하지 않는 행정동 코드입니다: 9999999999");
    }

    @Test
    @DisplayName("summary 요청 목록이 null이면 bad request 대상 예외를 낸다")
    void failsWhenFilteredSummaryRequestListIsNull() {
        assertThatThrownBy(() -> rentPriceService.getSummaries(null, null, null))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    private AdminDong adminDong(String adminDongCode, String adminDongName) {
        return new AdminDong(
                new AdminDongCsvRow(
                        adminDongCode,
                        "서울특별시",
                        "종로구",
                        adminDongName,
                        37.58,
                        126.97,
                        "20081101",
                        ""));
    }

    private RentPrice adminStat(
            AdminDong adminDong,
            String buildingType,
            Long medianSalePrice,
            Integer saleCount,
            Long medianJeonseDeposit,
            Integer jeonseCount,
            Long medianMonthlyDeposit,
            Long medianMonthlyRent,
            Integer monthlyRentCount) {
        return RentPrice.builder()
                .adminDong(adminDong)
                .buildingType(buildingType)
                .medianSalePrice(medianSalePrice)
                .avgSalePrice(medianSalePrice == null ? null : medianSalePrice + 1_000L)
                .avgSalePricePerSquareMeter(
                        medianSalePrice == null ? null : new BigDecimal("1200.10"))
                .saleCount(saleCount)
                .medianJeonseDeposit(medianJeonseDeposit)
                .avgJeonseDeposit(medianJeonseDeposit == null ? null : medianJeonseDeposit + 500L)
                .avgJeonseDepositPerSquareMeter(
                        medianJeonseDeposit == null ? null : new BigDecimal("800.20"))
                .jeonseCount(jeonseCount)
                .medianMonthlyDeposit(medianMonthlyDeposit)
                .avgMonthlyDeposit(
                        medianMonthlyDeposit == null ? null : medianMonthlyDeposit + 100L)
                .medianMonthlyRent(medianMonthlyRent)
                .avgMonthlyRent(medianMonthlyRent == null ? null : medianMonthlyRent + 5L)
                .monthlyRentCount(monthlyRentCount)
                .build();
    }
}
