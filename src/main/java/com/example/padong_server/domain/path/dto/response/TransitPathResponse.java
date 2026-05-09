package com.example.padong_server.domain.path.dto.response;

import com.example.padong_server.domain.dongne.dto.AdminDongDto;
import com.example.padong_server.domain.dongne.entity.AdminDong;
import com.example.padong_server.domain.path.dto.internal.OdsayJson;
import com.example.padong_server.domain.path.dto.internal.PathSummary;
import com.example.padong_server.domain.path.entity.PathSource;
import com.example.padong_server.global.exception.ErrorCode;
import com.example.padong_server.global.util.Preconditions;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@Schema(description = "행정동 A → 행정동 B 대중교통 길찾기 응답")
public class TransitPathResponse {

    @Schema(description = "출발 행정동")
    private AdminDongDto departureDong;

    @Schema(description = "도착 행정동")
    private AdminDongDto arrivalDong;

    @Schema(description = "총 소요시간(분)", example = "9")
    private int totalTime;

    @Schema(description = "총 거리(m)", example = "2000")
    private int totalDistance;

    public static TransitPathResponse of(
            AdminDong departureDong, AdminDong arrivalDong, PathSummary summary) {
        return TransitPathResponse.builder()
                .departureDong(AdminDongDto.from(departureDong))
                .arrivalDong(AdminDongDto.from(arrivalDong))
                .totalTime(summary.totalTime())
                .totalDistance(summary.totalDistance())
                .build();
    }

    public static PathSummary parseSummary(Map<String, Object> body) {
        Map<String, Object> result = OdsayJson.map(body.get("result"));
        Preconditions.validate(!result.isEmpty(), ErrorCode.ODSAY_NO_RESULT);

        List<Map<String, Object>> paths = OdsayJson.mapList(result.get("path"));
        Preconditions.validate(!paths.isEmpty(), ErrorCode.ODSAY_NO_RESULT);

        Map<String, Object> info = OdsayJson.map(paths.get(0).get("info"));
        int totalTime = OdsayJson.intValue(info.get("totalTime"), 0);
        int totalDistance = OdsayJson.intValue(info.get("totalDistance"), 0);
        return new PathSummary(totalTime, totalDistance, PathSource.ODSAY);
    }
}
