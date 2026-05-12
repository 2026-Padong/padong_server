package com.example.padong_server.domain.dongne.dto;

import com.example.padong_server.domain.dongne.entity.AdminDong;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "행정동 자동완성 항목")
public record DongSuggestionItem(
        @Schema(description = "행정동 코드", example = "1141051500") String adminDongCode,
        @Schema(description = "행정동 이름", example = "연희동") String name,
        @Schema(description = "자치구 이름", example = "서대문구") String guName,
        @Schema(description = "자치구 포함 풀 주소", example = "서울 서대문구 연희동") String fullAddress) {

    public static DongSuggestionItem from(AdminDong dong) {
        String full =
                String.join(" ", dong.getCityName(), dong.getDistrictName(), dong.getAdminDongName());
        return new DongSuggestionItem(
                dong.getAdminDongCode(),
                dong.getAdminDongName(),
                dong.getDistrictName(),
                full);
    }
}
