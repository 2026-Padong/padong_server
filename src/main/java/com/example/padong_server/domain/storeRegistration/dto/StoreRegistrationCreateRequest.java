package com.example.padong_server.domain.storeRegistration.dto;

import com.example.padong_server.domain.storeRegistration.entity.StoreCategory;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;

@Schema(description = "가게 등록 요청 (JSON body)")
public record StoreRegistrationCreateRequest(
        @Schema(description = "가게가 속한 행정동 코드. 미입력 시 address geocoding 으로 자동 매핑 시도 (geocoding 서비스 활성화 시).",
                example = "1162069500")
                String adminDongCode,
        @Schema(description = "가게명", example = "파동 식당")
                @NotBlank @Size(max = 100) String name,
        @Schema(description = "카테고리 enum code", example = "KOREAN")
                @NotNull StoreCategory category,
        @Schema(description = "전체 주소 (도로명 + 상세)", example = "서울 송파구 올림픽로 300 1층")
                @NotBlank @Size(max = 500) String address,
        @Schema(description = "가게 전화번호", example = "0507-2093-9485")
                @NotBlank @Size(max = 30) String phoneNumber,
        @Schema(description = "한 줄 소개 (선택)") @Size(max = 1000) String description,
        @Schema(description = "오픈 시간 (HH:mm)", example = "10:00")
                @NotNull
                @JsonFormat(pattern = "HH:mm")
                LocalTime openTime,
        @Schema(description = "마감 시간 (HH:mm)", example = "20:00")
                @NotNull
                @JsonFormat(pattern = "HH:mm")
                LocalTime closeTime,
        @Schema(description = "영업 요일 비트마스크 (bit0=MON..bit6=SUN, 0~127)", example = "62")
                @Min(0)
                @Max(127)
                int weekdayMask,
        @Schema(description = "위도 (geocoding 결과, 선택)", example = "37.5683")
                Double latitude,
        @Schema(description = "경도 (geocoding 결과, 선택)", example = "126.9261")
                Double longitude) {}
