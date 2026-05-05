package com.example.padong_server.domain.rentPrice.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "건물유형 정보")
public record ResidenceBuildingTypeResponse(
        @Schema(
                description = "건물유형 code",
                allowableValues = {"APARTMENT", "OFFICETEL", "ROW_MULTIFAMILY", "DETACHED_MULTIFAMILY"},
                example = "APARTMENT"
        )
        String buildingTypeCode,

        @Schema(
                description = "건물유형 한글 라벨",
                allowableValues = {"아파트", "오피스텔", "연립다세대", "단독다가구"},
                example = "아파트"
        )
        String buildingTypeLabel
) {
}
