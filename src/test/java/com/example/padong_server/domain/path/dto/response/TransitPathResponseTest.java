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

class TransitPathResponseTest {

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
    @DisplayName("ODsay 정상 응답에서 totalTime + totalDistance 추출")
    void parseSummary_validResponse_extractsTimeAndDistance() throws IOException {
        Map<String, Object> raw = parseJson(VALID_ODSAY_RESPONSE);

        PathSummary summary = TransitPathResponse.parseSummary(raw);

        assertThat(summary.totalTime()).isEqualTo(9);
        assertThat(summary.totalDistance()).isEqualTo(2000);
    }

    @Test
    @DisplayName("of() 로 응답 DTO 빌드")
    void of_buildsDto() {
        PathSummary summary = new PathSummary(9, 2000);

        TransitPathResponse response = TransitPathResponse.of(DEPARTURE, ARRIVAL, summary);

        assertThat(response.getDepartureDong().getAdminDongCode()).isEqualTo("1162069500");
        assertThat(response.getArrivalDong().getAdminDongCode()).isEqualTo("1168064000");
        assertThat(response.getTotalTime()).isEqualTo(9);
        assertThat(response.getTotalDistance()).isEqualTo(2000);
    }

    @Test
    @DisplayName("result 가 비어있으면 ODSAY_NO_RESULT 를 던진다")
    void parseSummary_emptyResult_throws() throws IOException {
        Map<String, Object> raw = parseJson("{\"result\": {}}");

        assertThatThrownBy(() -> TransitPathResponse.parseSummary(raw))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ODSAY_NO_RESULT);
    }

    @Test
    @DisplayName("path 배열이 비어있으면 ODSAY_NO_RESULT 를 던진다")
    void parseSummary_emptyPath_throws() throws IOException {
        Map<String, Object> raw = parseJson("{\"result\": {\"path\": []}}");

        assertThatThrownBy(() -> TransitPathResponse.parseSummary(raw))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ODSAY_NO_RESULT);
    }

    private Map<String, Object> parseJson(String json) throws IOException {
        @SuppressWarnings("unchecked")
        Map<String, Object> map = OBJECT_MAPPER.readValue(json, Map.class);
        return map;
    }

    private static final String VALID_ODSAY_RESPONSE =
            """
            {
              "result": {
                "path": [
                  {
                    "info": {
                      "totalTime": 9,
                      "totalDistance": 2000,
                      "payment": 1250
                    }
                  }
                ]
              }
            }
            """;
}
