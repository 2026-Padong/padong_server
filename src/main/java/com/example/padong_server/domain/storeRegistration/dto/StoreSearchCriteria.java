package com.example.padong_server.domain.storeRegistration.dto;

import com.example.padong_server.domain.storeRegistration.entity.ShopStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "가게 목록 검색·필터 조건")
public record StoreSearchCriteria(
        @Schema(description = "행정동 코드 (미지정 시 전체)", example = "1162069500")
                String adminDongCode,
        @Schema(description = "텍스트 검색 (가게명·카테고리·설명)", example = "베이커리") String q,
        @Schema(description = "상태 필터 (다중)", example = "[\"RECRUITING\",\"CLOSING\"]")
                List<ShopStatus> status,
        @Schema(description = "카테고리 필터 (다중)", example = "[\"카페, 디저트\",\"베이커리\"]")
                List<String> category,
        @Schema(description = "내가 좋아요한 가게만 (JWT 필요)", example = "false")
                Boolean likedOnly) {

    public boolean hasText() {
        return q != null && !q.isBlank();
    }

    public String trimmedQ() {
        return q == null ? null : q.trim();
    }
}
