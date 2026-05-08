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
    @DisplayName("ODsay 정상 응답을 요약 DTO로 변환한다")
    void from_validResponse_mapsToDto() throws IOException {
        Map<String, Object> raw = parseJson(VALID_ODSAY_RESPONSE);

        TransitPathResponse response = TransitPathResponse.from(DEPARTURE, ARRIVAL, raw);

        assertThat(response.getDepartureDong().getAdminDongCode()).isEqualTo("1162069500");
        assertThat(response.getDepartureDong().getAddress()).isEqualTo("서울특별시 관악구 신림동");
        assertThat(response.getArrivalDong().getAdminDongCode()).isEqualTo("1168064000");
        assertThat(response.getArrivalDong().getAddress()).isEqualTo("서울특별시 강남구 역삼1동");
        assertThat(response.getSearchType()).isEqualTo(0);
        assertThat(response.getPathCount()).isEqualTo(1);
        assertThat(response.getPaths()).hasSize(1);

        TransitPath path = response.getPaths().get(0);
        assertThat(path.getPathType()).isEqualTo(1);
        assertThat(path.getTotalTime()).isEqualTo(9);
        assertThat(path.getPayment()).isEqualTo(1250);
        assertThat(path.getTransferCount()).isEqualTo(1); // busTransitCount=0 + subwayTransitCount=1
        assertThat(path.getMapObj()).isEqualTo("2:2:237:238");

        // -1 sentinel은 null로 정규화
        assertThat(path.getTotalWalkTime()).isNull();
        assertThat(path.getTotalWalk()).isEqualTo(0);

        assertThat(path.getSubPaths()).hasSize(1);
        TransitSubPath subPath = path.getSubPaths().get(0);
        assertThat(subPath.getTrafficType()).isEqualTo(1);
        assertThat(subPath.getDistance()).isEqualTo(2000);
        assertThat(subPath.getSectionTime()).isEqualTo(9);
        assertThat(subPath.getStartName()).isEqualTo("당산");
        assertThat(subPath.getEndName()).isEqualTo("합정");
        assertThat(subPath.getLaneName()).isEqualTo("수도권 2호선");
        assertThat(subPath.getStationCount()).isEqualTo(1);
        assertThat(subPath.getStartCoord().getX()).isEqualTo(126.902682);
        assertThat(subPath.getStartCoord().getY()).isEqualTo(37.534863);
        assertThat(subPath.getEndCoord().getX()).isEqualTo(126.914543);
        assertThat(subPath.getEndCoord().getY()).isEqualTo(37.549942);
    }

    @Test
    @DisplayName("result 가 비어있으면 ODSAY_NO_RESULT 를 던진다")
    void from_emptyResult_throws() throws IOException {
        Map<String, Object> raw = parseJson("{\"result\": {}}");

        assertThatThrownBy(() -> TransitPathResponse.from(DEPARTURE, ARRIVAL, raw))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ODSAY_NO_RESULT);
    }

    @Test
    @DisplayName("path 배열이 비어있으면 ODSAY_NO_RESULT 를 던진다")
    void from_emptyPath_throws() throws IOException {
        Map<String, Object> raw = parseJson("{\"result\": {\"path\": [], \"searchType\": 0}}");

        assertThatThrownBy(() -> TransitPathResponse.from(DEPARTURE, ARRIVAL, raw))
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
                    "pathType": 1,
                    "subPath": [
                      {
                        "trafficType": 1,
                        "distance": 2000,
                        "sectionTime": 9,
                        "lane": {
                          "name": "수도권 2호선",
                          "subwayCode": 2,
                          "subwayCityCode": 1000
                        },
                        "stationCount": 1,
                        "startX": 126.902682,
                        "startY": 37.534863,
                        "startID": 237,
                        "startName": "당산",
                        "endX": 126.914543,
                        "endY": 37.549942,
                        "endID": 238,
                        "endName": "합정"
                      }
                    ],
                    "info": {
                      "mapObj": "2:2:237:238",
                      "payment": 1250,
                      "busTransitCount": 0,
                      "subwayTransitCount": 1,
                      "busStationCount": 0,
                      "subwayStationCount": 1,
                      "totalStationCount": 1,
                      "totalTime": 9,
                      "totalWalk": 0,
                      "trafficDistance": 2000,
                      "totalDistance": 2000,
                      "firstStartStation": "당산",
                      "lastEndStation": "합정",
                      "totalWalkTime": -1
                    }
                  }
                ],
                "searchType": 0,
                "startRadius": 700,
                "endRadius": 700,
                "subwayCount": 1,
                "busCount": 10,
                "subwayBusCount": 0,
                "pointDistance": 1967,
                "outTrafficCheck": 0
              }
            }
            """;
}
