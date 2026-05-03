package com.example.padong_server.domain.rentPrice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.padong_server.domain.dongne.dto.AdminDongCsvRow;
import com.example.padong_server.domain.dongne.dto.DongMappingCsvRow;
import com.example.padong_server.domain.dongne.dto.LegalDongCsvRow;
import com.example.padong_server.domain.dongne.entity.AdminDong;
import com.example.padong_server.domain.dongne.entity.DongMapping;
import com.example.padong_server.domain.dongne.entity.LegalDong;
import com.example.padong_server.domain.dongne.repository.DongMappingRepository;
import com.example.padong_server.domain.rentPrice.dto.internal.RentPriceRawData;
import com.example.padong_server.domain.rentPrice.dto.internal.RentPriceRawData.RentRow;
import com.example.padong_server.domain.rentPrice.dto.internal.RentPriceRawData.RentType;
import com.example.padong_server.domain.rentPrice.dto.internal.RentPriceRawData.SaleRow;
import com.example.padong_server.domain.rentPrice.dto.response.RentPriceImportResponse;
import com.example.padong_server.domain.rentPrice.entity.RentPrice;
import com.example.padong_server.domain.rentPrice.repository.RentPriceRepository;
import com.example.padong_server.domain.rentPrice.util.RentPriceDataUtil;
import com.example.padong_server.global.exception.CustomException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class RentPriceDataImportServiceTest {

    @Mock private RentPriceDataUtil rentPriceDataUtil;

    @Mock private DongMappingRepository dongMappingRepository;

    @Mock private RentPriceRepository rentPriceRepository;

    @InjectMocks private RentPriceDataImportService rentPriceDataImportService;

    @Test
    @DisplayName("법정동 원천 데이터를 행정동 기준 RentPrice로 변환해 저장한다")
    void importsAdminDongBasedRentPrices() {
        LegalDong legalDong = legalDong("1111010100");
        AdminDong firstAdminDong = adminDong("1111051500", "청운효자동");
        AdminDong secondAdminDong = adminDong("1111053000", "사직동");
        RentPriceRawData rawData =
                new RentPriceRawData(
                        List.of(
                                new SaleRow("1111010100", "아파트", 100L, new BigDecimal("10")),
                                new SaleRow("1111010100", "아파트", 201L, new BigDecimal("20"))),
                        List.of(
                                new RentRow(
                                        "1111010100",
                                        "아파트",
                                        RentType.JEONSE,
                                        50L,
                                        null,
                                        new BigDecimal("10")),
                                new RentRow(
                                        "1111010100",
                                        "아파트",
                                        RentType.MONTHLY_RENT,
                                        10L,
                                        1L,
                                        new BigDecimal("10"))),
                        4L,
                        1L);
        when(rentPriceDataUtil.readRows()).thenReturn(rawData);
        when(dongMappingRepository.findAllWithAdminAndLegal())
                .thenReturn(
                        List.of(
                                mapping(firstAdminDong, legalDong),
                                mapping(secondAdminDong, legalDong)));
        when(rentPriceRepository.saveAll(org.mockito.ArgumentMatchers.anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RentPriceImportResponse result = rentPriceDataImportService.importData();

        ArgumentCaptor<List<RentPrice>> statCaptor = ArgumentCaptor.forClass(List.class);
        verify(rentPriceRepository).deleteAllInBatch();
        verify(rentPriceRepository).saveAll(statCaptor.capture());

        List<RentPrice> stats = statCaptor.getValue();

        assertThat(result.savedStatCount()).isEqualTo(2);
        assertThat(result.sourceRowCount()).isEqualTo(4L);
        assertThat(result.saleRowCount()).isEqualTo(2L);
        assertThat(result.jeonseRowCount()).isEqualTo(1L);
        assertThat(result.monthlyRentRowCount()).isEqualTo(1L);
        assertThat(result.skippedRowCount()).isEqualTo(1L);

        assertThat(stats).hasSize(2);
        assertThat(stats)
                .extracting(stat -> stat.getAdminDong().getAdminDongCode())
                .containsExactly("1111051500", "1111053000");
        assertThat(stats).extracting(RentPrice::getMedianSalePrice).containsExactly(151L, 151L);
        assertThat(stats).extracting(RentPrice::getMedianJeonseDeposit).containsExactly(50L, 50L);
        assertThat(stats).extracting(RentPrice::getMedianMonthlyRent).containsExactly(1L, 1L);
    }

    @Test
    @DisplayName("월세 대표값은 보증금과 월세가 모두 중앙값에 가까운 실제 거래 쌍을 선택한다")
    void selectsMonthlyRentPairClosestToDepositAndRentMedians() {
        LegalDong legalDong = legalDong("1111010100");
        AdminDong adminDong = adminDong("1111051500", "청운효자동");
        RentPriceRawData rawData =
                new RentPriceRawData(
                        List.of(),
                        List.of(
                                new RentRow(
                                        "1111010100",
                                        "아파트",
                                        RentType.MONTHLY_RENT,
                                        500L,
                                        60L,
                                        new BigDecimal("10")),
                                new RentRow(
                                        "1111010100",
                                        "아파트",
                                        RentType.MONTHLY_RENT,
                                        1_000L,
                                        70L,
                                        new BigDecimal("10")),
                                new RentRow(
                                        "1111010100",
                                        "아파트",
                                        RentType.MONTHLY_RENT,
                                        50_000L,
                                        80L,
                                        new BigDecimal("10")),
                                new RentRow(
                                        "1111010100",
                                        "아파트",
                                        RentType.MONTHLY_RENT,
                                        1_200L,
                                        90L,
                                        new BigDecimal("10")),
                                new RentRow(
                                        "1111010100",
                                        "아파트",
                                        RentType.MONTHLY_RENT,
                                        1_500L,
                                        100L,
                                        new BigDecimal("10"))),
                        5L,
                        0L);
        when(rentPriceDataUtil.readRows()).thenReturn(rawData);
        when(dongMappingRepository.findAllWithAdminAndLegal())
                .thenReturn(List.of(mapping(adminDong, legalDong)));

        rentPriceDataImportService.importData();

        RentPrice stat = savedStat();
        assertThat(stat.getMedianMonthlyDeposit()).isEqualTo(1_200L);
        assertThat(stat.getMedianMonthlyRent()).isEqualTo(90L);
        assertThat(stat.getMonthlyRentCount()).isEqualTo(5);
    }

    @Test
    @DisplayName("월세 대표값 동점이면 낮은 월세와 보증금 순으로 선택한다")
    void breaksMonthlyRentPairTiesByLowerRentThenLowerDeposit() {
        LegalDong legalDong = legalDong("1111010100");
        AdminDong adminDong = adminDong("1111051500", "청운효자동");
        RentPriceRawData rawData =
                new RentPriceRawData(
                        List.of(),
                        List.of(
                                new RentRow(
                                        "1111010100",
                                        "아파트",
                                        RentType.MONTHLY_RENT,
                                        1_000L,
                                        70L,
                                        new BigDecimal("10")),
                                new RentRow(
                                        "1111010100",
                                        "아파트",
                                        RentType.MONTHLY_RENT,
                                        2_000L,
                                        90L,
                                        new BigDecimal("10"))),
                        2L,
                        0L);
        when(rentPriceDataUtil.readRows()).thenReturn(rawData);
        when(dongMappingRepository.findAllWithAdminAndLegal())
                .thenReturn(List.of(mapping(adminDong, legalDong)));

        rentPriceDataImportService.importData();

        RentPrice stat = savedStat();
        assertThat(stat.getMedianMonthlyDeposit()).isEqualTo(1_000L);
        assertThat(stat.getMedianMonthlyRent()).isEqualTo(70L);
    }

    @Test
    @DisplayName("월세 거래가 1건이면 해당 거래 쌍을 대표값으로 선택한다")
    void selectsOnlyMonthlyRentPairWhenSingleMonthlyRentExists() {
        LegalDong legalDong = legalDong("1111010100");
        AdminDong adminDong = adminDong("1111051500", "청운효자동");
        RentPriceRawData rawData =
                new RentPriceRawData(
                        List.of(),
                        List.of(
                                new RentRow(
                                        "1111010100",
                                        "아파트",
                                        RentType.MONTHLY_RENT,
                                        1_000L,
                                        80L,
                                        new BigDecimal("10"))),
                        1L,
                        0L);
        when(rentPriceDataUtil.readRows()).thenReturn(rawData);
        when(dongMappingRepository.findAllWithAdminAndLegal())
                .thenReturn(List.of(mapping(adminDong, legalDong)));

        rentPriceDataImportService.importData();

        RentPrice stat = savedStat();
        assertThat(stat.getMedianMonthlyDeposit()).isEqualTo(1_000L);
        assertThat(stat.getMedianMonthlyRent()).isEqualTo(80L);
        assertThat(stat.getMonthlyRentCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("월세 거래가 없으면 월세 대표값을 null로 저장한다")
    void storesNullMonthlyRentPairWhenMonthlyRentDoesNotExist() {
        LegalDong legalDong = legalDong("1111010100");
        AdminDong adminDong = adminDong("1111051500", "청운효자동");
        RentPriceRawData rawData =
                new RentPriceRawData(
                        List.of(new SaleRow("1111010100", "아파트", 100L, new BigDecimal("10"))),
                        List.of(),
                        1L,
                        0L);
        when(rentPriceDataUtil.readRows()).thenReturn(rawData);
        when(dongMappingRepository.findAllWithAdminAndLegal())
                .thenReturn(List.of(mapping(adminDong, legalDong)));

        rentPriceDataImportService.importData();

        RentPrice stat = savedStat();
        assertThat(stat.getMedianMonthlyDeposit()).isNull();
        assertThat(stat.getMedianMonthlyRent()).isNull();
        assertThat(stat.getMonthlyRentCount()).isZero();
    }

    @Test
    @DisplayName("행정동 매핑이 없는 법정동 코드가 있으면 기존 데이터를 삭제하지 않는다")
    void failsBeforeReplacingWhenAdminMappingIsMissing() {
        RentPriceRawData rawData =
                new RentPriceRawData(
                        List.of(new SaleRow("1111010100", "아파트", 100L, new BigDecimal("10"))),
                        List.of(),
                        1L,
                        0L);
        when(rentPriceDataUtil.readRows()).thenReturn(rawData);
        when(dongMappingRepository.findAllWithAdminAndLegal()).thenReturn(List.of());

        assertThatThrownBy(() -> rentPriceDataImportService.importData())
                .isInstanceOf(CustomException.class)
                .hasMessage("행정동 매핑이 없는 법정동 코드입니다: 1111010100");

        verifyNoInteractions(rentPriceRepository);
    }

    private RentPrice savedStat() {
        ArgumentCaptor<List<RentPrice>> statCaptor = ArgumentCaptor.forClass(List.class);
        verify(rentPriceRepository).saveAll(statCaptor.capture());
        return statCaptor.getValue().get(0);
    }

    private LegalDong legalDong(String legalDongCode) {
        return new LegalDong(
                new LegalDongCsvRow(legalDongCode, "서울특별시", "종로구", "청운동", "19880423", ""));
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

    private DongMapping mapping(AdminDong adminDong, LegalDong legalDong) {
        return new DongMapping(
                adminDong,
                legalDong,
                new DongMappingCsvRow(
                        adminDong.getAdminDongCode(),
                        "서울특별시",
                        "종로구",
                        adminDong.getAdminDongName(),
                        legalDong.getLegalDongCode(),
                        legalDong.getLegalDongName(),
                        "20081101",
                        ""));
    }
}
