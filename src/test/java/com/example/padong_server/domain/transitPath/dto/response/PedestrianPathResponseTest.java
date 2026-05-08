package com.example.padong_server.domain.transitPath.dto.response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.padong_server.domain.dongne.dto.AdminDongCsvRow;
import com.example.padong_server.domain.dongne.entity.AdminDong;
import com.example.padong_server.global.exception.CustomException;
import com.example.padong_server.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

class PedestrianPathResponseTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final AdminDong DEPARTURE =
            new AdminDong(
                    new AdminDongCsvRow(
                            "1162069500",
                            "서울특별시",
                            "관악구",
                            "신림동",
                            37.4842,
                            126.9295,
                            "20260325",
                            ""));

    private static final AdminDong ARRIVAL =
            new AdminDong(
                    new AdminDongCsvRow(
                            "1168064000",
                            "서울특별시",
                            "강남구",
                            "역삼1동",
                            37.4998,
                            127.0364,
                            "20260325",
                            ""));

    @Test
    @DisplayName("SP feature에서 totalTime/totalDistance 추출 + 초→분 반올림")
    void from_validResponse_extractsSummary() throws IOException {
        Map<String, Object> raw = parseJson(VALID_TMAP_RESPONSE);

        PedestrianPathResponse response = PedestrianPathResponse.from(DEPARTURE, ARRIVAL, raw);

        assertThat(response.getDepartureDong().getAdminDongCode()).isEqualTo("1162069500");
        assertThat(response.getArrivalDong().getAdminDongCode()).isEqualTo("1168064000");
        assertThat(response.getTotalDistance()).isEqualTo(1240);
        // 1067초 → 17.78분 → 반올림 18
        assertThat(response.getTotalTime()).isEqualTo(18);
    }

    @Test
    @DisplayName("초 단위 반올림 경계 검증")
    void from_secondsToMinutes_rounding() throws IOException {
        // 30초 → 0.5분 → 1 (반올림 half-up은 0.5에서 짝수로 갈 수 있음, Math.round는 half-up)
        Map<String, Object> raw30 = parseJson(buildResponse(30, 100));
        assertThat(PedestrianPathResponse.from(DEPARTURE, ARRIVAL, raw30).getTotalTime()).isEqualTo(1);

        // 29초 → 0.483분 → 0
        Map<String, Object> raw29 = parseJson(buildResponse(29, 100));
        assertThat(PedestrianPathResponse.from(DEPARTURE, ARRIVAL, raw29).getTotalTime()).isEqualTo(0);

        // 89초 → 1.483 → 1
        Map<String, Object> raw89 = parseJson(buildResponse(89, 100));
        assertThat(PedestrianPathResponse.from(DEPARTURE, ARRIVAL, raw89).getTotalTime()).isEqualTo(1);

        // 90초 → 1.5 → 2
        Map<String, Object> raw90 = parseJson(buildResponse(90, 100));
        assertThat(PedestrianPathResponse.from(DEPARTURE, ARRIVAL, raw90).getTotalTime()).isEqualTo(2);
    }

    @Test
    @DisplayName("SP feature가 없으면 SK_PEDESTRIAN_NO_RESULT 를 던진다")
    void from_noStartFeature_throws() throws IOException {
        Map<String, Object> raw = parseJson("{\"type\":\"FeatureCollection\",\"features\":[]}");

        assertThatThrownBy(() -> PedestrianPathResponse.from(DEPARTURE, ARRIVAL, raw))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SK_PEDESTRIAN_NO_RESULT);
    }

    private Map<String, Object> parseJson(String json) throws IOException {
        @SuppressWarnings("unchecked")
        Map<String, Object> map = OBJECT_MAPPER.readValue(json, Map.class);
        return map;
    }

    private String buildResponse(int totalTimeSeconds, int totalDistanceMeters) {
        return ("""
                {
                  "type": "FeatureCollection",
                  "features": [
                    {
                      "type": "Feature",
                      "geometry": {"type": "Point", "coordinates": [126.9, 37.5]},
                      "properties": {
                        "pointType": "SP",
                        "totalTime": %d,
                        "totalDistance": %d
                      }
                    }
                  ]
                }
                """).formatted(totalTimeSeconds, totalDistanceMeters);
    }

    private static final String VALID_TMAP_RESPONSE =
            """
            {
              "type": "FeatureCollection",
              "features": [
                {
                  "type": "Feature",
                  "geometry": {"type": "Point", "coordinates": [126.9027279, 37.5349277]},
                  "properties": {
                    "totalDistance": 1240,
                    "totalTime": 1067,
                    "index": 0,
                    "pointType": "SP",
                    "name": "관악구 신림동"
                  }
                },
                {
                  "type": "Feature",
                  "geometry": {"type": "LineString", "coordinates": [[126.9, 37.5],[126.91, 37.51]]},
                  "properties": {
                    "index": 1,
                    "lineIndex": 0,
                    "distance": 300,
                    "time": 240,
                    "roadType": 21
                  }
                },
                {
                  "type": "Feature",
                  "geometry": {"type": "Point", "coordinates": [127.0364, 37.4998]},
                  "properties": {
                    "index": 5,
                    "pointType": "EP",
                    "name": "강남구 역삼1동"
                  }
                }
              ]
            }
            """;
}
