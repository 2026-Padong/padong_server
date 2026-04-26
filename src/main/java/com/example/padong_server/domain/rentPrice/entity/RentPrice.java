package com.example.padong_server.domain.rentPrice.entity;

import com.example.padong_server.domain.dongne.entity.LegalDong;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "rent_price",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_rent_price_legal_dong_building_type",
                columnNames = {"legal_dong_id", "building_type"}
        )
)
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RentPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "legal_dong_id", nullable = false)
    private LegalDong legalDong;

    @Column(name = "building_type", nullable = false, length = 50)
    private String buildingType;

    // 매매 평균가(만원)
    @Column(name = "avg_sale_price")
    private Long avgSalePrice;

    // 매매 중위가(만원)
    @Column(name = "median_sale_price")
    private Long medianSalePrice;

    // 매매 평균 ㎡당 가격(만원/㎡)
    @Column(name = "avg_sale_price_per_square_meter", precision = 12, scale = 2)
    private BigDecimal avgSalePricePerSquareMeter;

    // 매매 거래건수(건)
    @Column(name = "sale_count")
    private Integer saleCount;

    // 전세 평균 보증금(만원)
    @Column(name = "avg_jeonse_deposit")
    private Long avgJeonseDeposit;

    // 전세 중위 보증금(만원)
    @Column(name = "median_jeonse_deposit")
    private Long medianJeonseDeposit;

    // 전세 평균 ㎡당 보증금(만원/㎡)
    @Column(name = "avg_jeonse_deposit_per_square_meter", precision = 12, scale = 2)
    private BigDecimal avgJeonseDepositPerSquareMeter;

    // 전세 거래건수(건)
    @Column(name = "jeonse_count")
    private Integer jeonseCount;

    // 월세 평균 보증금(만원)
    @Column(name = "avg_monthly_deposit")
    private Long avgMonthlyDeposit;

    // 월세 중위 보증금(만원)
    @Column(name = "median_monthly_deposit")
    private Long medianMonthlyDeposit;

    // 월세 평균 월세금(만원)
    @Column(name = "avg_monthly_rent")
    private Long avgMonthlyRent;

    // 월세 중위 월세금(만원)
    @Column(name = "median_monthly_rent")
    private Long medianMonthlyRent;

    // 월세 거래건수(건)
    @Column(name = "monthly_rent_count")
    private Integer monthlyRentCount;
}
