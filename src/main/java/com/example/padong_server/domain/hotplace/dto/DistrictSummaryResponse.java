package com.example.padong_server.domain.hotplace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "자치구 실시간 요약 (날씨·날씨 대표 POI)")
public class DistrictSummaryResponse {

    @Schema(description = "자치구 한글명", example = "종로구")
    private final String guName;

    @Schema(description = "summary 산정 기준 대표 POI 이름", example = "광화문·덕수궁")
    private final String selectedAreaNm;

    @Schema(description = "날씨/대기 요약")
    private final WeatherSummary summary;
}
