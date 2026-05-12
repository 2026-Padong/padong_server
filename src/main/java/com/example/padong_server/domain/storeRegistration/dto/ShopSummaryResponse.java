package com.example.padong_server.domain.storeRegistration.dto;

import com.example.padong_server.domain.payment.entity.GroupOrder;
import com.example.padong_server.domain.storeRegistration.entity.RecruitmentStatus;
import com.example.padong_server.domain.storeRegistration.entity.Store;
import com.example.padong_server.domain.storeRegistration.entity.StoreCategory;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "가게 카드 요약 (목록용)")
public record ShopSummaryResponse(
        @Schema(description = "가게 PK", example = "12") Long id,
        @Schema(description = "가게명", example = "베이커리 가든") String name,
        @Schema(description = "썸네일 이미지 URL", example = "https://.../thumb.jpg") String thumbnailUrl,
        @Schema(description = "카테고리 enum code", example = "BAKERY") StoreCategory category,
        @Schema(description = "카테고리 표시 라벨", example = "베이커리") String categoryLabel,
        @Schema(description = "한 줄 소개", example = "대화하기 좋은 베이커리 카페") String description,
        @Schema(description = "현재 참여자 수 (active 공구 기준, 없으면 0)", example = "1")
                int participantCurrent,
        @Schema(description = "모집 정원 (active 공구 기준, 없으면 null)", example = "5")
                Integer participantTotal,
        @Schema(description = "현재 사용자 좋아요 여부 (Heart 채움)", example = "true")
                boolean likedByCurrentUser,
        @Schema(description = "가게 위도 (geocoding 미완료 시 null)", example = "37.5683")
                Double latitude,
        @Schema(description = "가게 경도 (geocoding 미완료 시 null)", example = "126.9261")
                Double longitude,
        @Schema(description = "영업 요일 비트마스크 (bit0=MON..bit6=SUN)", example = "62")
                int weekdayMask,
        @Schema(
                description =
                        "사용자 측 모집 상태 (RECRUITING / CLOSING_SOON / IN_PROGRESS / NO_FLOW / OUT_OF_HOURS).",
                example = "RECRUITING")
                RecruitmentStatus recruitmentStatus) {

    public static ShopSummaryResponse from(
            Store store,
            GroupOrder activeGroupOrder,
            boolean likedByCurrentUser,
            RecruitmentStatus recruitmentStatus) {
        int current = activeGroupOrder == null ? 0 : activeGroupOrder.getCurrentParticipants();
        Integer total = activeGroupOrder == null ? null : activeGroupOrder.getMaxParticipants();
        return new ShopSummaryResponse(
                store.getId(),
                store.getName(),
                store.getThumbnailUrl(),
                store.getCategory(),
                store.getCategory() == null ? null : store.getCategory().getLabel(),
                store.getDescription(),
                current,
                total,
                likedByCurrentUser,
                store.getLatitude(),
                store.getLongitude(),
                store.getWeekdayMask(),
                recruitmentStatus);
    }
}
