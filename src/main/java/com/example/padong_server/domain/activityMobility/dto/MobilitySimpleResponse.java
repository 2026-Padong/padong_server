package com.example.padong_server.domain.activityMobility.dto;

import com.example.padong_server.domain.activityMobility.entity.Mobility;
import com.example.padong_server.domain.dongne.dto.AdminDongDto;
import com.example.padong_server.domain.dongne.entity.AdminDong;
import com.example.padong_server.domain.rentPrice.dto.response.SelectedRentPriceResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import tools.jackson.databind.JsonNode;

@Getter
@Builder
@Schema(description = "단일 행정동 생활이동 / 취향 추천 카드 item")
public class MobilitySimpleResponse {

    @Schema(description = "출발 후보 행정동 (취향 추천에서는 '추천 동네')")
    private AdminDongDto departureDong;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "최근 3개월 일평균 생활이동 총합 (취향 추천에서는 null)", example = "477.07")
    private Double totalMobility;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "평균 이동시간 (취향 추천에서는 null)", example = "42.7")
    private Double avgTime;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "출발 동네 안전등급", example = "B")
    private String safetyGrade;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "선택 주거/가격 정보")
    private SelectedRentPriceResponse rentPrice;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "총 인구수 (취향 추천 카드용)", example = "23514.0")
    private Double totalPopulation;

    @Schema(description = "행정동 polygon 경계 (GeoJSON Feature). 매핑 없으면 null")
    private JsonNode boundary;

    @Schema(description = "좋아요 수", example = "12")
    private long likeCount;

    @Schema(description = "현재 사용자의 좋아요 여부 (비로그인은 false)", example = "true")
    private boolean likedByCurrentUser;

    public static MobilitySimpleResponse from(
            Mobility mobility,
            String safetyGrade,
            SelectedRentPriceResponse rentPrice,
            JsonNode boundary,
            long likeCount,
            boolean likedByCurrentUser) {
        AdminDong departureDong = mobility.getDepartureDong();
        return MobilitySimpleResponse.builder()
                .departureDong(AdminDongDto.from(departureDong))
                .totalMobility(roundToSecondDecimal(mobility.getTotalMobility()))
                .avgTime(roundToSecondDecimal(mobility.getAvgTime()))
                .safetyGrade(safetyGrade)
                .rentPrice(rentPrice)
                .boundary(boundary)
                .likeCount(likeCount)
                .likedByCurrentUser(likedByCurrentUser)
                .build();
    }

    /** 취향 추천용 — Mobility 엔티티 없이 AdminDong 만으로 카드 빌드. totalMobility/avgTime 은 null. */
    public static MobilitySimpleResponse forRecommendation(
            AdminDong adminDong,
            String safetyGrade,
            SelectedRentPriceResponse rentPrice,
            Double totalPopulation,
            JsonNode boundary,
            long likeCount,
            boolean likedByCurrentUser) {
        return MobilitySimpleResponse.builder()
                .departureDong(AdminDongDto.from(adminDong))
                .safetyGrade(safetyGrade)
                .rentPrice(rentPrice)
                .totalPopulation(totalPopulation)
                .boundary(boundary)
                .likeCount(likeCount)
                .likedByCurrentUser(likedByCurrentUser)
                .build();
    }

    private static double roundToSecondDecimal(double value) {
        return Math.round(value * 100) / 100.0;
    }
}
