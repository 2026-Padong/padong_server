package com.example.padong_server.domain.dongneLike.dto;

import com.example.padong_server.domain.dongne.entity.AdminDong;
import com.example.padong_server.domain.dongneLike.entity.DongneLike;
import com.example.padong_server.domain.population.entity.PopulationDensity;
import com.example.padong_server.domain.rentPrice.entity.RentPrice;
import io.swagger.v3.oas.annotations.media.Schema;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

@Schema(description = "내가 좋아요한 동네 항목 (동네 카드 렌더용)")
public record LikedDongneResponse(
        @Schema(description = "DongneLike PK (= cursor 값)", example = "12") Long likeId,
        @Schema(description = "행정동 코드", example = "1162069500") String adminDongCode,
        @Schema(description = "자치구명", example = "관악구") String guName,
        @Schema(description = "행정동명", example = "신림동") String name,
        @Schema(description = "풀 주소 (cityName + districtName + adminDongName)",
                example = "서울특별시 관악구 신림동")
                String fullAddress,
        @Schema(
                description =
                        "행정동 통계 기반 태그. 데이터 있는 항목만 노출 (인구·밀도·월세). "
                                + "사용자별 라이프스타일과 무관 — 같은 동네는 모든 사용자에게 동일.",
                example = "[\"인구 41,250명\", \"밀도 23,150명/km²\", \"월세 500/45\"]")
                List<String> tags) {

    private static final DecimalFormat THOUSANDS = new DecimalFormat("#,###");

    public static LikedDongneResponse from(
            DongneLike like, PopulationDensity density, List<RentPrice> rentRows) {
        AdminDong d = like.getAdminDong();
        String full =
                String.join(" ", d.getCityName(), d.getDistrictName(), d.getAdminDongName());
        return new LikedDongneResponse(
                like.getId(),
                d.getAdminDongCode(),
                d.getDistrictName(),
                d.getAdminDongName(),
                full,
                buildTags(density, rentRows));
    }

    private static List<String> buildTags(PopulationDensity density, List<RentPrice> rentRows) {
        List<String> tags = new ArrayList<>();
        if (density != null) {
            tags.add("인구 " + THOUSANDS.format((long) density.getTotalPopulation()) + "명");
            tags.add("밀도 " + THOUSANDS.format((long) density.getDensity()) + "명/km²");
        }
        if (rentRows != null && !rentRows.isEmpty()) {
            Long avgDeposit = average(rentRows, RentPrice::getAvgMonthlyDeposit);
            Long avgRent = average(rentRows, RentPrice::getAvgMonthlyRent);
            if (avgDeposit != null && avgRent != null) {
                tags.add("월세 " + avgDeposit + "/" + avgRent);
            }
        }
        return tags;
    }

    private static Long average(
            List<RentPrice> rows, java.util.function.Function<RentPrice, Long> selector) {
        long sum = 0;
        int count = 0;
        for (RentPrice row : rows) {
            Long value = selector.apply(row);
            if (value != null) {
                sum += value;
                count++;
            }
        }
        return count == 0 ? null : sum / count;
    }
}
