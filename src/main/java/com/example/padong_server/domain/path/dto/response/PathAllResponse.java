package com.example.padong_server.domain.path.dto.response;

import com.example.padong_server.domain.dongne.dto.AdminDongDto;
import com.example.padong_server.domain.dongne.entity.AdminDong;
import com.example.padong_server.domain.path.dto.internal.PathSummary;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "행정동 A → 행정동 B 통합 길찾기 응답 (3개 모드)")
public class PathAllResponse {

    @Schema(description = "출발 행정동")
    private AdminDongDto departureDong;

    @Schema(description = "도착 행정동")
    private AdminDongDto arrivalDong;

    @Schema(description = "모드별 경로 결과 (대중교통 / 보행자 / 자동차)")
    private Paths paths;

    public static PathAllResponse of(
            AdminDong departureDong,
            AdminDong arrivalDong,
            PathSummary transit,
            PathSummary pedestrian,
            PathSummary car) {
        return PathAllResponse.builder()
                .departureDong(AdminDongDto.from(departureDong))
                .arrivalDong(AdminDongDto.from(arrivalDong))
                .paths(Paths.builder().transit(transit).pedestrian(pedestrian).car(car).build())
                .build();
    }

    @Getter
    @Builder
    @Schema(description = "모드별 경로 결과")
    public static class Paths {

        @Schema(description = "대중교통 (totalTime 분, totalDistance m)")
        private PathSummary transit;

        @Schema(description = "보행자 (totalTime 분, totalDistance m)")
        private PathSummary pedestrian;

        @Schema(description = "자동차 (totalTime 분, totalDistance m)")
        private PathSummary car;
    }
}
