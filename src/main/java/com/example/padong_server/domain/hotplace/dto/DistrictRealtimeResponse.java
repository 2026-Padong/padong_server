package com.example.padong_server.domain.hotplace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class DistrictRealtimeResponse {

    @Schema(description = "조회한 자치구 이름", example = "용산구")
    private final String districtName;

    @Schema(description = "대표로 선택된 핫플레이스 이름", example = "국립중앙박물관")
    private final String selectedAreaName;

    private final WeatherSummary summary;

    @Schema(description = "자치구 내 핫플레이스 실시간 정보 목록")
    private final List<HotplaceRealtimeItem> hotplaces;
}
