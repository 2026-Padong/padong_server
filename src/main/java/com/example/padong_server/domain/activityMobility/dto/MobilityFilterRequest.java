package com.example.padong_server.domain.activityMobility.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "생활이동 조회 필터. 가격 단위는 만원.")
public class MobilityFilterRequest {

    @Schema(description = "단일 조회 평균 이동시간 최소값", example = "30")
    private Double minAvgTime;

    @Schema(description = "단일 조회 평균 이동시간 최대값", example = "60")
    private Double maxAvgTime;

    @Schema(description = "다중 조회 각 도착지별 평균 이동시간 최소값", example = "30")
    private Double minEachAvgTime;

    @Schema(description = "다중 조회 각 도착지별 평균 이동시간 최대값", example = "60")
    private Double maxEachAvgTime;

    @Schema(
            description = "출발 후보 행정동의 자치구 이름 목록. DB admin_dong.district_name 값과 완전 일치.",
            example = "[\"관악구\", \"동작구\"]"
    )
    private List<String> departureDistrictNames;

    @Schema(
            description = "거래 형태. 한글 라벨 또는 enum code 사용 가능.",
            allowableValues = {"SALE", "JEONSE", "MONTHLY_RENT", "매매", "전세", "월세"},
            example = "MONTHLY_RENT"
    )
    private String contractType;

    @Schema(
            description = "집 형태. 한글 라벨 또는 enum code 사용 가능.",
            allowableValues = {"APARTMENT", "OFFICETEL", "ROW_MULTIFAMILY", "DETACHED_MULTIFAMILY", "아파트", "오피스텔", "연립다세대", "단독다가구"},
            example = "OFFICETEL"
    )
    private String houseType;

    @Schema(description = "매매가 최소값. 단위는 만원.", example = "100000")
    private Long minSalePrice;

    @Schema(description = "매매가 최대값. 단위는 만원.", example = "250000")
    private Long maxSalePrice;

    @Schema(description = "전세 보증금 최소값. 단위는 만원.", example = "10000")
    private Long minJeonseDeposit;

    @Schema(description = "전세 보증금 최대값. 단위는 만원.", example = "30000")
    private Long maxJeonseDeposit;

    @Schema(description = "월세 보증금 최소값. 단위는 만원.", example = "1000")
    private Long minMonthlyDeposit;

    @Schema(description = "월세 보증금 최대값. 단위는 만원.", example = "5000")
    private Long maxMonthlyDeposit;

    @Schema(description = "월세금 최소값. 단위는 만원.", example = "50")
    private Long minMonthlyRent;

    @Schema(description = "월세금 최대값. 단위는 만원.", example = "80")
    private Long maxMonthlyRent;

    public static MobilityFilterRequest empty() {
        return MobilityFilterRequest.builder().build();
    }
}
