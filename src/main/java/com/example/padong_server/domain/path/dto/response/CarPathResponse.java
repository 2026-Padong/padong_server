package com.example.padong_server.domain.path.dto.response;

import com.example.padong_server.domain.dongne.dto.AdminDongDto;
import com.example.padong_server.domain.dongne.entity.AdminDong;
import com.example.padong_server.domain.path.dto.internal.OdsayJson;
import com.example.padong_server.domain.path.dto.internal.PathSummary;
import com.example.padong_server.global.exception.ErrorCode;
import com.example.padong_server.global.util.Preconditions;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Getter
@Builder
@Schema(description = "행정동 A → 행정동 B 자동차 경로 응답")
public class CarPathResponse {

    private static final String START_POINT_TYPE = "S";
    private static final int SECONDS_PER_MINUTE = 60;

    @Schema(description = "출발 행정동")
    private AdminDongDto departureDong;

    @Schema(description = "도착 행정동")
    private AdminDongDto arrivalDong;

    @Schema(description = "총 소요시간(분, 반올림)", example = "18")
    private int totalTime;

    @Schema(description = "총 거리(m)", example = "12500")
    private int totalDistance;

    public static CarPathResponse of(
            AdminDong departureDong, AdminDong arrivalDong, PathSummary summary) {
        return CarPathResponse.builder()
                .departureDong(AdminDongDto.from(departureDong))
                .arrivalDong(AdminDongDto.from(arrivalDong))
                .totalTime(summary.totalTime())
                .totalDistance(summary.totalDistance())
                .build();
    }

    public static PathSummary parseSummary(Map<String, Object> body) {
        List<Map<String, Object>> features = OdsayJson.mapList(body.get("features"));
        Map<String, Object> startProperties = findStartFeatureProperties(features);

        int totalTimeSeconds = OdsayJson.intValue(startProperties.get("totalTime"), 0);
        int totalDistance = OdsayJson.intValue(startProperties.get("totalDistance"), 0);
        return new PathSummary(roundSecondsToMinutes(totalTimeSeconds), totalDistance);
    }

    private static Map<String, Object> findStartFeatureProperties(
            List<Map<String, Object>> features) {
        for (Map<String, Object> feature : features) {
            Map<String, Object> properties = OdsayJson.map(feature.get("properties"));
            if (START_POINT_TYPE.equals(Objects.toString(properties.get("pointType"), ""))) {
                return properties;
            }
        }
        Preconditions.validate(false, ErrorCode.SK_CAR_NO_RESULT);
        return Map.of();
    }

    private static int roundSecondsToMinutes(int seconds) {
        return (int) Math.round((double) seconds / SECONDS_PER_MINUTE);
    }
}
