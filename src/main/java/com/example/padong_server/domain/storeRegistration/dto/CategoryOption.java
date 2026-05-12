package com.example.padong_server.domain.storeRegistration.dto;

import com.example.padong_server.domain.storeRegistration.entity.StoreCategory;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "가게 카테고리 dropdown 옵션")
public record CategoryOption(
        @Schema(description = "enum code (DB 저장값·요청 송신값)", example = "BAKERY") String code,
        @Schema(description = "UI 표시 라벨", example = "베이커리") String label) {

    public static CategoryOption from(StoreCategory category) {
        return new CategoryOption(category.name(), category.getLabel());
    }
}
