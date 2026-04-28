package com.example.padong_server.domain.hotplace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HotplaceRealtimeItem {

    @Schema(description = "핫플레이스 이름", example = "국립중앙박물관")
    private final String areaName;

    private final String thumbnail;

    @Schema(description = "도로명 주소", example = "서울 용산구 서빙고로 137")
    private final String roadAddress;

    @Schema(description = "예상 최소 인구", example = "12000")
    private final String minPopulation;

    @Schema(description = "예상 최대 인구", example = "18000")
    private final String maxPopulation;

    private final String congestionLevel;
    private final String dominantAgeGroup;
    private final String dominantAgeRate;

    @Schema(description = "주변 도로 소통 상태", example = "원활")
    private final String roadTrafficStatus;

    @Schema(description = "주변 도로 평균 속도(km/h)", example = "42.5")
    private final String roadTrafficSpeed;
}
