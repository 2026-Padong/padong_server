package com.example.padong_server.domain.storeRegistration.dto;

import com.example.padong_server.domain.storeRegistration.entity.StoreCategory;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;

@Schema(description = "가게 부분 수정 요청 (JSON body). null 필드는 변경 없음 (PATCH).")
public record StoreRegistrationUpdateRequest(
        @Schema(description = "행정동 코드", example = "1162069500") String adminDongCode,
        @Schema(description = "가게명") @Size(max = 100) String name,
        @Schema(description = "카테고리 enum code", example = "KOREAN") StoreCategory category,
        @Schema(description = "전체 주소") @Size(max = 500) String address,
        @Schema(description = "전화번호") @Size(max = 30) String phoneNumber,
        @Schema(description = "한 줄 소개") @Size(max = 1000) String description,
        @Schema(description = "오픈 시간 (HH:mm)") @JsonFormat(pattern = "HH:mm") LocalTime openTime,
        @Schema(description = "마감 시간 (HH:mm)") @JsonFormat(pattern = "HH:mm") LocalTime closeTime,
        @Schema(description = "요일 마스크 (0~127)", example = "62") @Min(0) @Max(127) Integer weekdayMask,
        @Schema(description = "위도") Double latitude,
        @Schema(description = "경도") Double longitude) {}
