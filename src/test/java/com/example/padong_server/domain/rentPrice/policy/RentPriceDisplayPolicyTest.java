package com.example.padong_server.domain.rentPrice.policy;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.padong_server.domain.dongne.entity.LegalDong;
import com.example.padong_server.domain.rentPrice.entity.RentPrice;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RentPriceDisplayPolicyTest {

    private final RentPriceDisplayPolicy rentPriceDisplayPolicy = new RentPriceDisplayPolicy();

    @Test
    @DisplayName("매매 대표값은 중위값을 평균보다 우선 사용한다")
    void prefersMedianForSaleDisplay() {
        RentPrice stat = baseStatBuilder()
                .medianSalePrice(98_000L)
                .avgSalePrice(100_000L)
                .avgSalePricePerSquareMeter(new BigDecimal("1200.12"))
                .saleCount(35)
                .build();

        RentPriceDisplayPolicy.PriceDecision decision = rentPriceDisplayPolicy.decideSale(stat);

        assertThat(decision.source()).isEqualTo(RentPriceDisplayPolicy.MetricSource.MEDIAN);
        assertThat(decision.amount()).isEqualTo(98_000L);
        assertThat(decision.amountPerSquareMeter()).isNull();
    }

    @Test
    @DisplayName("전세 대표값은 중위값이 없으면 평균, 평균도 없으면 ㎡당 가격으로 fallback 한다")
    void fallsBackForJeonseDisplay() {
        RentPrice stat = baseStatBuilder()
                .avgJeonseDepositPerSquareMeter(new BigDecimal("820.55"))
                .jeonseCount(7)
                .build();

        RentPriceDisplayPolicy.PriceDecision decision = rentPriceDisplayPolicy.decideJeonse(stat);

        assertThat(decision.source()).isEqualTo(RentPriceDisplayPolicy.MetricSource.AVERAGE_PER_SQUARE_METER);
        assertThat(decision.amount()).isNull();
        assertThat(decision.amountPerSquareMeter()).isEqualByComparingTo(new BigDecimal("820.55"));
    }

    @Test
    @DisplayName("월세 대표값은 보증금과 월세금을 중위값 세트로 우선 사용한다")
    void prefersMedianPairForMonthlyRentDisplay() {
        RentPrice stat = baseStatBuilder()
                .medianMonthlyDeposit(1_000L)
                .medianMonthlyRent(85L)
                .avgMonthlyDeposit(1_200L)
                .avgMonthlyRent(90L)
                .monthlyRentCount(12)
                .build();

        RentPriceDisplayPolicy.MonthlyRentDecision decision = rentPriceDisplayPolicy.decideMonthlyRent(stat);

        assertThat(decision.source()).isEqualTo(RentPriceDisplayPolicy.MetricSource.MEDIAN);
        assertThat(decision.deposit()).isEqualTo(1_000L);
        assertThat(decision.monthlyRent()).isEqualTo(85L);
    }

    @Test
    @DisplayName("데이터가 전혀 없으면 none 상태로 분류한다")
    void classifiesNoDataWhenNoValuesExist() {
        RentPrice stat = baseStatBuilder().build();

        RentPriceDisplayPolicy.PriceDecision saleDecision = rentPriceDisplayPolicy.decideSale(stat);
        RentPriceDisplayPolicy.MonthlyRentDecision monthlyDecision = rentPriceDisplayPolicy.decideMonthlyRent(stat);

        assertThat(saleDecision.source()).isEqualTo(RentPriceDisplayPolicy.MetricSource.NONE);
        assertThat(monthlyDecision.source()).isEqualTo(RentPriceDisplayPolicy.MetricSource.NONE);
    }

    private RentPrice.RentPriceBuilder baseStatBuilder() {
        return RentPrice.builder()
                .legalDong(new LegalDong())
                .buildingType("아파트");
    }
}
