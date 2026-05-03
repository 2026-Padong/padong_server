package com.example.padong_server.domain.activityMobility.dto;

import com.example.padong_server.domain.rentPrice.dto.request.RentPriceFilterCriteria;
import com.example.padong_server.domain.rentPrice.entity.RentPriceTradeType;
import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.PositiveOrZero;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "생활이동 조회 필터, 가격 단위 만원")
public class MobilityFilterRequest {

    private static final String INVALID_RANGE_MESSAGE = "필터 범위의 최소값은 최대값보다 클 수 없습니다.";
    private static final String NEGATIVE_TIME_MESSAGE = "시간 범위 값은 0 이상이어야 합니다.";
    private static final String NEGATIVE_PRICE_MESSAGE = "가격 범위 값은 0 이상이어야 합니다.";
    private static final String INVALID_PRICE_FILTER_MESSAGE = "거래 형태와 가격 필터 조합이 올바르지 않습니다.";

    @Schema(description = "단일 조회 평균 이동시간 최소값", example = "30")
    @PositiveOrZero(message = NEGATIVE_TIME_MESSAGE)
    private Double minAvgTime;

    @Schema(description = "단일 조회 평균 이동시간 최대값", example = "60")
    @PositiveOrZero(message = NEGATIVE_TIME_MESSAGE)
    private Double maxAvgTime;

    @Schema(description = "다중 조회 각 도착지별 평균 이동시간 최소값", example = "30")
    @PositiveOrZero(message = NEGATIVE_TIME_MESSAGE)
    private Double minEachAvgTime;

    @Schema(description = "다중 조회 각 도착지별 평균 이동시간 최대값", example = "60")
    @PositiveOrZero(message = NEGATIVE_TIME_MESSAGE)
    private Double maxEachAvgTime;

    @Schema(
            description = "출발 후보 행정동 자치구 이름 목록, DB admin_dong.district_name 완전 일치",
            example = "[\"관악구\", \"동작구\"]")
    private List<String> departureDistrictNames;

    @Schema(
            description = "거래 형태 enum code",
            allowableValues = {"SALE", "JEONSE", "MONTHLY_RENT"},
            example = "MONTHLY_RENT")
    private String contractType;

    @Schema(
            description = "주거 형태 enum code",
            allowableValues = {"APARTMENT", "OFFICETEL", "ROW_MULTIFAMILY", "DETACHED_MULTIFAMILY"},
            example = "OFFICETEL")
    private String houseType;

    @Schema(description = "매매가 최소값, 단위 만원, contractType=SALE 전용, 예: 100000")
    @PositiveOrZero(message = NEGATIVE_PRICE_MESSAGE)
    private Long minSalePrice;

    @Schema(description = "매매가 최대값, 단위 만원, contractType=SALE 전용, 예: 250000")
    @PositiveOrZero(message = NEGATIVE_PRICE_MESSAGE)
    private Long maxSalePrice;

    @Schema(description = "전세 보증금 최소값, 단위 만원, contractType=JEONSE 전용, 예: 10000")
    @PositiveOrZero(message = NEGATIVE_PRICE_MESSAGE)
    private Long minJeonseDeposit;

    @Schema(description = "전세 보증금 최대값, 단위 만원, contractType=JEONSE 전용, 예: 30000")
    @PositiveOrZero(message = NEGATIVE_PRICE_MESSAGE)
    private Long maxJeonseDeposit;

    @Schema(description = "월세 보증금 최소값, 단위 만원, contractType=MONTHLY_RENT 전용", example = "1000")
    @PositiveOrZero(message = NEGATIVE_PRICE_MESSAGE)
    private Long minMonthlyDeposit;

    @Schema(description = "월세 보증금 최대값, 단위 만원, contractType=MONTHLY_RENT 전용", example = "5000")
    @PositiveOrZero(message = NEGATIVE_PRICE_MESSAGE)
    private Long maxMonthlyDeposit;

    @Schema(description = "월세금 최소값, 단위 만원, contractType=MONTHLY_RENT 전용", example = "50")
    @PositiveOrZero(message = NEGATIVE_PRICE_MESSAGE)
    private Long minMonthlyRent;

    @Schema(description = "월세금 최대값, 단위 만원, contractType=MONTHLY_RENT 전용", example = "80")
    @PositiveOrZero(message = NEGATIVE_PRICE_MESSAGE)
    private Long maxMonthlyRent;

    public static MobilityFilterRequest empty() {
        return MobilityFilterRequest.builder().build();
    }

    public static MobilityFilterRequest emptyIfNull(MobilityFilterRequest filterRequest) {
        return filterRequest == null ? empty() : filterRequest;
    }

    public boolean hasSingleFilter() {
        return hasRange(minAvgTime, maxAvgTime)
                || hasDepartureDistrictNameFilter()
                || hasRentPriceFilter();
    }

    public boolean hasRentPriceFilter() {
        return hasText(contractType) || hasText(houseType) || hasAnyPriceFilter();
    }

    private boolean hasAnyPriceFilter() {
        return hasSalePriceFilter() || hasJeonsePriceFilter() || hasMonthlyPriceFilter();
    }

    private boolean hasSalePriceFilter() {
        return hasRange(minSalePrice, maxSalePrice);
    }

    private boolean hasJeonsePriceFilter() {
        return hasRange(minJeonseDeposit, maxJeonseDeposit);
    }

    private boolean hasMonthlyPriceFilter() {
        return hasRange(minMonthlyDeposit, maxMonthlyDeposit)
                || hasRange(minMonthlyRent, maxMonthlyRent);
    }

    @JsonIgnore
    public RentPriceFilterCriteria toRentPriceFilterCriteria() {
        return RentPriceFilterCriteria.from(
                contractType,
                houseType,
                minSalePrice,
                maxSalePrice,
                minJeonseDeposit,
                maxJeonseDeposit,
                minMonthlyDeposit,
                maxMonthlyDeposit,
                minMonthlyRent,
                maxMonthlyRent);
    }

    @JsonIgnore
    @AssertTrue(message = INVALID_RANGE_MESSAGE)
    public boolean isRangeValid() {
        return !isInvalidRange(minAvgTime, maxAvgTime)
                && !isInvalidRange(minEachAvgTime, maxEachAvgTime)
                && !isInvalidRange(minSalePrice, maxSalePrice)
                && !isInvalidRange(minJeonseDeposit, maxJeonseDeposit)
                && !isInvalidRange(minMonthlyDeposit, maxMonthlyDeposit)
                && !isInvalidRange(minMonthlyRent, maxMonthlyRent);
    }

    @JsonIgnore
    @AssertTrue(message = INVALID_PRICE_FILTER_MESSAGE)
    public boolean isPriceFilterValid() {
        RentPriceTradeType tradeType = contractTypeOrDefaultIfValid();
        if (tradeType == null) {
            return true;
        }

        return switch (tradeType) {
            case SALE -> !hasJeonsePriceFilter() && !hasMonthlyPriceFilter();
            case JEONSE -> !hasSalePriceFilter() && !hasMonthlyPriceFilter();
            case MONTHLY_RENT -> !hasSalePriceFilter() && !hasJeonsePriceFilter();
        };
    }

    private boolean hasDepartureDistrictNameFilter() {
        return departureDistrictNames != null
                && departureDistrictNames.stream().anyMatch(MobilityFilterRequest::hasText);
    }

    private static boolean hasRange(Object min, Object max) {
        return min != null || max != null;
    }

    private static <T extends Comparable<T>> boolean isInvalidRange(T min, T max) {
        return min != null && max != null && min.compareTo(max) > 0;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private RentPriceTradeType contractTypeOrDefaultIfValid() {
        try {
            return RentPriceTradeType.fromNullableOrDefault(
                    contractType, RentPriceFilterCriteria.DEFAULT_TRADE_TYPE);
        } catch (RuntimeException exception) {
            return null;
        }
    }
}
