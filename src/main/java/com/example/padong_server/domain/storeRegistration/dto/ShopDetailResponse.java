package com.example.padong_server.domain.storeRegistration.dto;

import com.example.padong_server.domain.menu.entity.Menu;
import com.example.padong_server.domain.orderFlow.entity.OrderFlow;
import com.example.padong_server.domain.payment.entity.GroupOrder;
import com.example.padong_server.domain.storeRegistration.entity.RecruitmentStatus;
import com.example.padong_server.domain.storeRegistration.entity.Store;
import com.example.padong_server.domain.storeRegistration.entity.StoreCategory;
import com.example.padong_server.domain.storeRegistration.entity.StoreImage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Schema(description = "가게 상세 응답")
public record ShopDetailResponse(
        @Schema(description = "가게 PK", example = "12") Long id,
        @Schema(description = "가게명", example = "연희제빵소") String name,
        @Schema(description = "카테고리 enum code", example = "BAKERY") StoreCategory category,
        @Schema(description = "카테고리 표시 라벨", example = "베이커리") String categoryLabel,
        @Schema(description = "썸네일 이미지 URL") String thumbnailUrl,
        @Schema(description = "헤더 Heart 채움 여부", example = "true") boolean likedByCurrentUser,
        @Schema(description = "한 줄 소개") String description,
        @Schema(description = "전체 주소") String address,
        @Schema(description = "전화번호") String phoneNumber,
        @Schema(description = "오픈 시간 (HH:mm)") String openTime,
        @Schema(description = "마감 시간 (HH:mm)") String closeTime,
        @Schema(description = "요일 비트마스크 (bit0=MON..bit6=SUN)") int weekdayMask,
        @Schema(description = "갤러리 이미지 (sort_order 오름차순)") List<StoreImageResponse> images,
        @Schema(description = "메뉴 리스트") List<ShopMenuItemResponse> menus,
        @Schema(description = "현재 참여자 수 (active 공구 기준)") int participantCurrent,
        @Schema(description = "모집 정원 (active 공구 기준)") Integer participantTotal,
        @Schema(description = "현재 활성 모임 요약. 없으면 null") CurrentGroupOrderSummary currentGroupOrder,
        @Schema(description = "가게 위도", example = "37.5683") Double latitude,
        @Schema(description = "가게 경도", example = "126.9261") Double longitude,
        @Schema(
                description =
                        "사용자 측 모집 상태 (RECRUITING / CLOSING_SOON / IN_PROGRESS / NO_FLOW / OUT_OF_HOURS).",
                example = "RECRUITING")
                RecruitmentStatus recruitmentStatus) {

    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");

    public static ShopDetailResponse from(
            Store store,
            GroupOrder activeGroupOrder,
            OrderFlow activeOrderFlow,
            List<Menu> menus,
            List<StoreImage> galleryImages,
            boolean likedByCurrentUser,
            RecruitmentStatus recruitmentStatus) {
        int current = activeGroupOrder == null ? 0 : activeGroupOrder.getCurrentParticipants();
        Integer total = activeGroupOrder == null ? null : activeGroupOrder.getMaxParticipants();

        List<ShopMenuItemResponse> menuResponses =
                menus.stream().map(ShopMenuItemResponse::from).toList();
        List<StoreImageResponse> imageResponses =
                galleryImages.stream().map(StoreImageResponse::from).toList();

        return new ShopDetailResponse(
                store.getId(),
                store.getName(),
                store.getCategory(),
                store.getCategory() == null ? null : store.getCategory().getLabel(),
                store.getThumbnailUrl(),
                likedByCurrentUser,
                store.getDescription(),
                store.getAddress(),
                store.getPhoneNumber(),
                formatTime(store.getOpenTime()),
                formatTime(store.getCloseTime()),
                store.getWeekdayMask(),
                imageResponses,
                menuResponses,
                current,
                total,
                CurrentGroupOrderSummary.from(activeOrderFlow),
                store.getLatitude(),
                store.getLongitude(),
                recruitmentStatus);
    }

    private static String formatTime(LocalTime time) {
        return time == null ? null : time.format(HHMM);
    }
}
