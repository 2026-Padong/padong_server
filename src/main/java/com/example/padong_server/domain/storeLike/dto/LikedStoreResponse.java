package com.example.padong_server.domain.storeLike.dto;

import com.example.padong_server.domain.storeLike.entity.StoreLike;
import com.example.padong_server.domain.storeRegistration.entity.Store;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "내가 좋아요한 가게 항목")
public record LikedStoreResponse(
        @Schema(description = "StoreLike PK", example = "7") Long likeId,
        @Schema(description = "Store PK", example = "12") Long storeId,
        @Schema(description = "가게 이름", example = "맛있는 김밥") String name,
        @Schema(description = "도로명 주소", example = "서울 관악구 신림로 1") String roadAddress,
        @Schema(description = "카테고리", example = "분식") String category) {

    public static LikedStoreResponse from(StoreLike like) {
        Store s = like.getStore();
        return new LikedStoreResponse(
                like.getId(), s.getId(), s.getName(), s.getRoadAddress(), s.getCategory());
    }
}
