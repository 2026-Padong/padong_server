package com.example.padong_server.domain.dongne.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "동네 요약 정보")
public class DongneSummaryResponse {

    @Schema(description = "행정동 코드", example = "1141061500")
    private String adminDongCode;

    @Schema(description = "시/도", example = "서울특별시")
    private String cityName;

    @Schema(description = "구", example = "마포구")
    private String districtName;

    @Schema(description = "행정동명", example = "연남동")
    private String adminDongName;

    @Schema(description = "전체 주소", example = "서울특별시 마포구 연남동")
    private String address;

    @Schema(description = "위도", example = "37.5623")
    private Double latitude;

    @Schema(description = "경도", example = "126.9235")
    private Double longitude;
}
