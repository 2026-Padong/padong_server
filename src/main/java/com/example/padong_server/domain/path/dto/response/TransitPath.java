package com.example.padong_server.domain.path.dto.response;

import com.example.padong_server.domain.path.dto.internal.OdsayJson;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@Schema(description = "추천 경로 1개")
public class TransitPath {

    @Schema(description = "경로 종류 (1: 지하철, 2: 버스, 3: 버스+지하철)", example = "1")
    private int pathType;

    @Schema(description = "총 소요시간(분)", example = "9")
    private int totalTime;

    @Schema(description = "총 도보 거리(m). 정보 없을 시 null", example = "0", nullable = true)
    private Integer totalWalk;

    @Schema(description = "총 도보 시간(분). 정보 없을 시 null", example = "0", nullable = true)
    private Integer totalWalkTime;

    @Schema(description = "예상 운임(원)", example = "1250")
    private int payment;

    @Schema(description = "환승 횟수 (버스+지하철 합산)", example = "0")
    private int transferCount;

    @Schema(description = "노선그래픽 호출용 식별자", example = "2:2:237:238")
    private String mapObj;

    @Schema(description = "경로 단계 목록")
    private List<TransitSubPath> subPaths;

    static TransitPath from(Map<String, Object> raw) {
        Map<String, Object> info = OdsayJson.map(raw.get("info"));
        int busTransit = OdsayJson.intValue(info.get("busTransitCount"), 0);
        int subwayTransit = OdsayJson.intValue(info.get("subwayTransitCount"), 0);

        List<TransitSubPath> subPaths = OdsayJson.mapList(raw.get("subPath")).stream()
                .map(TransitSubPath::from)
                .toList();

        return TransitPath.builder()
                .pathType(OdsayJson.intValue(raw.get("pathType"), 0))
                .totalTime(OdsayJson.intValue(info.get("totalTime"), 0))
                .totalWalk(OdsayJson.nullableNonNegativeInt(info.get("totalWalk")))
                .totalWalkTime(OdsayJson.nullableNonNegativeInt(info.get("totalWalkTime")))
                .payment(OdsayJson.intValue(info.get("payment"), 0))
                .transferCount(busTransit + subwayTransit)
                .mapObj(OdsayJson.stringValue(info.get("mapObj")))
                .subPaths(subPaths)
                .build();
    }
}
