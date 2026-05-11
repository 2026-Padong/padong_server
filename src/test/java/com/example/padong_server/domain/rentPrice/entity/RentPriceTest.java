package com.example.padong_server.domain.rentPrice.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.padong_server.domain.dongne.entity.AdminDong;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RentPriceTest {

    @Test
    @DisplayName("행정동과 건물유형 조합을 유일하게 저장한다")
    void declaresAdminDongAndBuildingTypeUniqueConstraint() {
        Table table = RentPrice.class.getAnnotation(Table.class);

        assertThat(table.name()).isEqualTo("rent_price");
        assertThat(table.uniqueConstraints()).hasSize(1);

        UniqueConstraint uniqueConstraint = table.uniqueConstraints()[0];
        assertThat(uniqueConstraint.name())
                .isEqualTo("uk_rent_price_admin_dong_building_type");
        assertThat(uniqueConstraint.columnNames())
                .containsExactly("admin_dong_id", "building_type");
    }

    @Test
    @DisplayName("행정동은 필수 Lazy 연관으로 매핑한다")
    void mapsAdminDongAsRequiredLazyAssociation() throws NoSuchFieldException {
        Field adminDongField = RentPrice.class.getDeclaredField("adminDong");

        ManyToOne manyToOne = adminDongField.getAnnotation(ManyToOne.class);
        JoinColumn joinColumn = adminDongField.getAnnotation(JoinColumn.class);

        assertThat(manyToOne.fetch()).isEqualTo(FetchType.LAZY);
        assertThat(manyToOne.optional()).isFalse();
        assertThat(joinColumn.name()).isEqualTo("admin_dong_id");
        assertThat(joinColumn.nullable()).isFalse();
    }

    @Test
    @DisplayName("㎡당 가격은 소수 둘째 자리까지 저장할 수 있게 매핑한다")
    void mapsPerSquareMeterPricesWithPrecisionAndScale() throws NoSuchFieldException {
        Column salePerSquareMeter = column("avgSalePricePerSquareMeter");
        Column jeonsePerSquareMeter = column("avgJeonseDepositPerSquareMeter");

        assertThat(salePerSquareMeter.precision()).isEqualTo(12);
        assertThat(salePerSquareMeter.scale()).isEqualTo(2);
        assertThat(jeonsePerSquareMeter.precision()).isEqualTo(12);
        assertThat(jeonsePerSquareMeter.scale()).isEqualTo(2);
    }

    @Test
    @DisplayName("최근 2년 매매/전세/월세 통계 필드를 보존한다")
    void storesSaleJeonseAndMonthlyRentStats() {
        AdminDong adminDong = new AdminDong();

        RentPrice stat = RentPrice.builder()
                .adminDong(adminDong)
                .buildingType("아파트")
                .avgSalePrice(100_000L)
                .medianSalePrice(98_000L)
                .avgSalePricePerSquareMeter(new BigDecimal("1234.56"))
                .saleCount(30)
                .avgJeonseDeposit(60_000L)
                .medianJeonseDeposit(58_000L)
                .avgJeonseDepositPerSquareMeter(new BigDecimal("789.10"))
                .jeonseCount(25)
                .avgMonthlyDeposit(10_000L)
                .medianMonthlyDeposit(8_000L)
                .avgMonthlyRent(120L)
                .medianMonthlyRent(110L)
                .monthlyRentCount(40)
                .build();

        assertThat(stat.getAdminDong()).isSameAs(adminDong);
        assertThat(stat.getBuildingType()).isEqualTo("아파트");
        assertThat(stat.getAvgSalePrice()).isEqualTo(100_000L);
        assertThat(stat.getMedianSalePrice()).isEqualTo(98_000L);
        assertThat(stat.getAvgSalePricePerSquareMeter())
                .isEqualByComparingTo(new BigDecimal("1234.56"));
        assertThat(stat.getSaleCount()).isEqualTo(30);
        assertThat(stat.getAvgJeonseDeposit()).isEqualTo(60_000L);
        assertThat(stat.getMedianJeonseDeposit()).isEqualTo(58_000L);
        assertThat(stat.getAvgJeonseDepositPerSquareMeter())
                .isEqualByComparingTo(new BigDecimal("789.10"));
        assertThat(stat.getJeonseCount()).isEqualTo(25);
        assertThat(stat.getAvgMonthlyDeposit()).isEqualTo(10_000L);
        assertThat(stat.getMedianMonthlyDeposit()).isEqualTo(8_000L);
        assertThat(stat.getAvgMonthlyRent()).isEqualTo(120L);
        assertThat(stat.getMedianMonthlyRent()).isEqualTo(110L);
        assertThat(stat.getMonthlyRentCount()).isEqualTo(40);
    }

    private Column column(String fieldName) throws NoSuchFieldException {
        return RentPrice.class
                .getDeclaredField(fieldName)
                .getAnnotation(Column.class);
    }
}
