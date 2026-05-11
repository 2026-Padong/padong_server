package com.example.padong_server.domain.dongneLike.dto;

import com.example.padong_server.domain.dongne.entity.AdminDong;
import com.example.padong_server.domain.dongneLike.entity.DongneLike;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "내가 좋아요한 동네 항목")
public record LikedDongneResponse(
        @Schema(description = "DongneLike PK", example = "12") Long likeId,
        @Schema(description = "행정동 코드", example = "1162069500") String adminDongCode,
        @Schema(description = "자치구명", example = "관악구") String guName,
        @Schema(description = "행정동명", example = "신림동") String name) {

    public static LikedDongneResponse from(DongneLike like) {
        AdminDong d = like.getAdminDong();
        return new LikedDongneResponse(
                like.getId(),
                d.getAdminDongCode(),
                d.getDistrictName(),
                d.getAdminDongName());
    }
}
