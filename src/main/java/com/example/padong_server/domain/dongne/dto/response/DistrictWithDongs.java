package com.example.padong_server.domain.dongne.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "자치구 + 소속 행정동 목록")
public record DistrictWithDongs(
        @Schema(description = "자치구명", example = "마포구") String guName,
        @Schema(description = "행정동 목록 (가나다순)") List<AdminDongItem> dongs) {

    @Schema(description = "행정동 항목")
    public record AdminDongItem(
            @Schema(description = "AdminDong PK (회원가입 시 그대로 전달)", example = "1") Long id,
            @Schema(description = "행정동명", example = "망원1동") String name,
            @Schema(description = "행정동 코드", example = "1144055500") String adminDongCode) {}
}
