package com.example.padong_server.domain.storeLike.dto;

import com.example.padong_server.domain.orderFlow.entity.OrderFlow;
import com.example.padong_server.domain.storeLike.entity.StoreLike;
import com.example.padong_server.domain.storeRegistration.entity.RecruitmentStatus;
import com.example.padong_server.domain.storeRegistration.entity.Store;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "내가 좋아요한 가게 항목 (가게 카드 렌더용)")
public record LikedStoreResponse(
        @Schema(description = "StoreLike PK (= cursor 값)", example = "7") Long likeId,
        @Schema(description = "Store PK", example = "12") Long storeId,
        @Schema(description = "가게 이름", example = "맛있는 김밥") String name,
        @Schema(description = "전체 주소", example = "서울 관악구 신림로 1") String address,
        @Schema(description = "카테고리 enum code", example = "SNACK") String category,
        @Schema(description = "썸네일 이미지 URL") String thumbnailUrl,
        @Schema(description = "한 줄 소개") String description,
        @Schema(
                description =
                        "사용자 측 모집 상태 (RECRUITING / CLOSING_SOON / IN_PROGRESS / NO_FLOW / OUT_OF_HOURS).",
                example = "RECRUITING")
                RecruitmentStatus recruitmentStatus,
        @Schema(description = "현재 참여자 수 (active 공구 없으면 null)") Integer participantCurrent,
        @Schema(description = "모집 정원 (active 공구 없으면 null)") Integer participantTotal,
        @Schema(description = "가게 위도 (geocoding 미완료 시 null)", example = "37.5683")
                Double latitude,
        @Schema(description = "가게 경도 (geocoding 미완료 시 null)", example = "126.9261")
                Double longitude) {

    public static LikedStoreResponse from(
            StoreLike like, OrderFlow active, RecruitmentStatus recruitmentStatus) {
        Store s = like.getStore();
        Integer cur = active == null ? null : active.getCurrentParticipants();
        Integer tot = active == null ? null : active.getMaxParticipants();
        return new LikedStoreResponse(
                like.getId(),
                s.getId(),
                s.getName(),
                s.getAddress(),
                s.getCategory() == null ? null : s.getCategory().name(),
                s.getThumbnailUrl(),
                s.getDescription(),
                recruitmentStatus,
                cur,
                tot,
                s.getLatitude(),
                s.getLongitude());
    }
}
