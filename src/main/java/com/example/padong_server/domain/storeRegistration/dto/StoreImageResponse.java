package com.example.padong_server.domain.storeRegistration.dto;

import com.example.padong_server.domain.storeRegistration.entity.StoreImage;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "가게 상세 갤러리 이미지 항목")
public record StoreImageResponse(
        @Schema(description = "이미지 PK", example = "13") Long id,
        @Schema(description = "이미지 URL") String url,
        @Schema(description = "정렬 순서 (오름차순)", example = "0") int order) {

    public static StoreImageResponse from(StoreImage image) {
        return new StoreImageResponse(image.getId(), image.getUrl(), image.getSortOrder());
    }
}
