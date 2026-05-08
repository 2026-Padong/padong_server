package com.example.padong_server.domain.transitPath.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
@Schema(description = "경로의 단일 구간")
public class TransitSubPath {

    @Schema(description = "교통수단 종류 (1: 지하철, 2: 버스, 3: 도보)", example = "1")
    private int trafficType;

    @Schema(description = "구간 거리(m)", example = "2000")
    private int distance;

    @Schema(description = "구간 소요시간(분)", example = "9")
    private int sectionTime;

    @Schema(description = "출발 정류장/역", example = "당산")
    private String startName;

    @Schema(description = "도착 정류장/역", example = "합정")
    private String endName;

    @Schema(description = "출발 정류장 좌표")
    private TransitCoord startCoord;

    @Schema(description = "도착 정류장 좌표")
    private TransitCoord endCoord;

    @Schema(description = "노선명", example = "수도권 2호선")
    private String laneName;

    @Schema(description = "정차 정거장 수", example = "1")
    private int stationCount;

    static TransitSubPath from(Map<String, Object> raw) {
        Map<String, Object> lane = OdsayJson.firstMap(raw.get("lane"));
        return TransitSubPath.builder()
                .trafficType(OdsayJson.intValue(raw.get("trafficType"), 0))
                .distance(OdsayJson.intValue(raw.get("distance"), 0))
                .sectionTime(OdsayJson.intValue(raw.get("sectionTime"), 0))
                .startName(OdsayJson.stringValue(raw.get("startName")))
                .endName(OdsayJson.stringValue(raw.get("endName")))
                .startCoord(TransitCoord.from(raw.get("startX"), raw.get("startY")))
                .endCoord(TransitCoord.from(raw.get("endX"), raw.get("endY")))
                .laneName(OdsayJson.stringValue(lane.get("name")))
                .stationCount(OdsayJson.intValue(raw.get("stationCount"), 0))
                .build();
    }
}
