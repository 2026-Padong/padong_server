package com.example.padong_server.domain.dongne.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "행정동 요약 정보")
public class AdminDongDto {

    @Schema(description = "행정동 코드", example = "1162069500")
    private String adminDongCode;

    @Schema(description = "행정동 주소", example = "서울특별시 관악구 신림동")
    private String address;
}
