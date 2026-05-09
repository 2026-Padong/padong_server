package com.example.padong_server.domain.path.dto.response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.padong_server.domain.dongne.dto.AdminDongCsvRow;
import com.example.padong_server.domain.dongne.entity.AdminDong;
import com.example.padong_server.domain.path.dto.internal.PathSummary;
import com.example.padong_server.global.exception.CustomException;
import com.example.padong_server.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

class CarPathResponseTest {

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
    @DisplayName("S feature에서 totalTime/totalDistance 추출 + 초→분 반올림")
    void parseSummary_extractsAndRounds() throws IOException {
        Map<String, Object> raw = parseJson(VALID_TMAP_CAR_RESPONSE);

        PathSummary summary = CarPathResponse.parseSummary(raw);

        assertThat(summary.totalDistance()).isEqualTo(12500);
        assertThat(summary.totalTime()).isEqualTo(18);
    }

    @Test
    @DisplayName("초 단위 반올림 경계 검증")
    void parseSummary_secondsToMinutes_rounding() throws IOException {
        assertThat(roundedFrom(30)).isEqualTo(1);
        assertThat(roundedFrom(29)).isEqualTo(0);
        assertThat(roundedFrom(89)).isEqualTo(1);
        assertThat(roundedFrom(90)).isEqualTo(2);
    }

    @Test
    @DisplayName("of() 로 응답 DTO 빌드")
    void of_buildsDto() {
        PathSummary summary = new PathSummary(18, 12500);

        CarPathResponse response = CarPathResponse.of(DEPARTURE, ARRIVAL, summary);

        assertThat(response.getDepartureDong().getAdminDongCode()).isEqualTo("1162069500");
        assertThat(response.getArrivalDong().getAdminDongCode()).isEqualTo("1168064000");
        assertThat(response.getTotalTime()).isEqualTo(18);
        assertThat(response.getTotalDistance()).isEqualTo(12500);
    }

    @Test
    @DisplayName("S feature가 없으면 SK_CAR_NO_RESULT 를 던진다")
    void parseSummary_noStartFeature_throws() throws IOException {
        Map<String, Object> raw = parseJson("{\"type\":\"FeatureCollection\",\"features\":[]}");

        assertThatThrownBy(() -> CarPathResponse.parseSummary(raw))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SK_CAR_NO_RESULT);
    }

    private int roundedFrom(int totalTimeSeconds) throws IOException {
        Map<String, Object> raw = parseJson(buildResponse(totalTimeSeconds, 100));
        return CarPathResponse.parseSummary(raw).totalTime();
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
                        "pointType": "S",
                        "totalTime": %d,
                        "totalDistance": %d
                      }
                    }
                  ]
                }
                """).formatted(totalTimeSeconds, totalDistanceMeters);
    }

    private static final String VALID_TMAP_CAR_RESPONSE =
            """
            {
              "type": "FeatureCollection",
              "features": [
                {
                  "type": "Feature",
                  "geometry": {"type": "Point", "coordinates": [126.9295, 37.4842]},
                  "properties": {
                    "totalDistance": 12500,
                    "totalTime": 1067,
                    "pointType": "S"
                  }
                }
              ]
            }
            """;
}
