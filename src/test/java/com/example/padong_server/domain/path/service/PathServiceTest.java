package com.example.padong_server.domain.path.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.example.padong_server.domain.dongne.dto.AdminDongCsvRow;
import com.example.padong_server.domain.dongne.entity.AdminDong;
import com.example.padong_server.domain.dongne.repository.AdminDongRepository;
import com.example.padong_server.domain.path.dto.request.CarPathRequest;
import com.example.padong_server.domain.path.dto.request.PedestrianPathRequest;
import com.example.padong_server.domain.path.dto.request.TransitPathRequest;
import com.example.padong_server.domain.path.dto.response.CarPathResponse;
import com.example.padong_server.domain.path.dto.response.PedestrianPathResponse;
import com.example.padong_server.domain.path.dto.response.TransitPathResponse;
import com.example.padong_server.global.client.odsay.OdsayClient;
import com.example.padong_server.global.client.sk.SkCarRouteClient;
import com.example.padong_server.global.client.sk.SkPedestrianRouteClient;
import com.example.padong_server.global.exception.CustomException;
import com.example.padong_server.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class PathServiceTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mock
    private AdminDongRepository adminDongRepository;

    @Mock
    private OdsayClient odsayClient;

    @Mock
    private SkPedestrianRouteClient skPedestrianRouteClient;

    @Mock
    private SkCarRouteClient skCarRouteClient;

    @InjectMocks
    private PathService pathService;

    @Nested
    @DisplayName("대중교통 길찾기 (searchTransit)")
    class Transit {

        @Test
        @DisplayName("정상 흐름: AdminDong 좌표를 OdsayClient에 전달하고 응답을 DTO로 변환")
        void search_validRequest_returnsResponse() throws IOException {
            AdminDong departure = adminDong("1162069500", "관악구", "신림동", 37.4842, 126.9295);
            AdminDong arrival = adminDong("1168064000", "강남구", "역삼1동", 37.4998, 127.0364);

            given(adminDongRepository.getByAdminDongCode("1162069500")).willReturn(departure);
            given(adminDongRepository.getByAdminDongCode("1168064000")).willReturn(arrival);
            given(
                            odsayClient.searchPubTransPath(
                                    eq(126.9295), eq(37.4842), eq(127.0364), eq(37.4998),
                                    eq((Integer) null), eq((Integer) null)))
                    .willReturn(parseJson(SAMPLE_ODSAY_RESPONSE));

            TransitPathRequest request =
                    TransitPathRequest.builder()
                            .departureDongCode("1162069500")
                            .arrivalDongCode("1168064000")
                            .build();

            TransitPathResponse response = pathService.searchTransit(request);

            assertThat(response.getDepartureDong().getAdminDongCode()).isEqualTo("1162069500");
            assertThat(response.getArrivalDong().getAdminDongCode()).isEqualTo("1168064000");
            assertThat(response.getPathCount()).isEqualTo(1);
            assertThat(response.getPaths().get(0).getTotalTime()).isEqualTo(9);
        }

        @Test
        @DisplayName("출발 코드와 도착 코드가 같으면 TRANSIT_PATH_SAME_DONG")
        void search_sameDongCode_throws() {
            TransitPathRequest request =
                    TransitPathRequest.builder()
                            .departureDongCode("1162069500")
                            .arrivalDongCode("1162069500")
                            .build();

            assertThatThrownBy(() -> pathService.searchTransit(request))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TRANSIT_PATH_SAME_DONG);
        }

        @Test
        @DisplayName("앞뒤 공백이 있어도 trim 후 비교하여 같으면 막는다")
        void search_sameDongCodeWithWhitespace_throws() {
            TransitPathRequest request =
                    TransitPathRequest.builder()
                            .departureDongCode(" 1162069500 ")
                            .arrivalDongCode("1162069500")
                            .build();

            assertThatThrownBy(() -> pathService.searchTransit(request))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TRANSIT_PATH_SAME_DONG);
        }
    }

    @Nested
    @DisplayName("보행자 경로 (searchPedestrian)")
    class Pedestrian {

        @Test
        @DisplayName("정상 흐름: AdminDong → 클라이언트에 좌표/이름 전달 → DTO 변환")
        void search_validRequest_returnsResponse() throws IOException {
            AdminDong departure = adminDong("1162069500", "관악구", "신림동", 37.4842, 126.9295);
            AdminDong arrival = adminDong("1168064000", "강남구", "역삼1동", 37.4998, 127.0364);

            given(adminDongRepository.getByAdminDongCode("1162069500")).willReturn(departure);
            given(adminDongRepository.getByAdminDongCode("1168064000")).willReturn(arrival);
            given(
                            skPedestrianRouteClient.route(
                                    eq("신림동"),
                                    eq(126.9295),
                                    eq(37.4842),
                                    eq("역삼1동"),
                                    eq(127.0364),
                                    eq(37.4998)))
                    .willReturn(parseJson(SAMPLE_TMAP_PEDESTRIAN_RESPONSE));

            PedestrianPathRequest request =
                    PedestrianPathRequest.builder()
                            .departureDongCode("1162069500")
                            .arrivalDongCode("1168064000")
                            .build();

            PedestrianPathResponse response = pathService.searchPedestrian(request);

            assertThat(response.getDepartureDong().getAdminDongCode()).isEqualTo("1162069500");
            assertThat(response.getArrivalDong().getAdminDongCode()).isEqualTo("1168064000");
            assertThat(response.getTotalDistance()).isEqualTo(1240);
            assertThat(response.getTotalTime()).isEqualTo(18);
        }

        @Test
        @DisplayName("출발 코드와 도착 코드가 같으면 TRANSIT_PATH_SAME_DONG")
        void search_sameDongCode_throws() {
            PedestrianPathRequest request =
                    PedestrianPathRequest.builder()
                            .departureDongCode("1162069500")
                            .arrivalDongCode("1162069500")
                            .build();

            assertThatThrownBy(() -> pathService.searchPedestrian(request))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TRANSIT_PATH_SAME_DONG);
        }
    }

    @Nested
    @DisplayName("자동차 경로 (searchCar)")
    class Car {

        @Test
        @DisplayName("정상 흐름: AdminDong → 클라이언트에 좌표/이름 전달 → DTO 변환")
        void search_validRequest_returnsResponse() throws IOException {
            AdminDong departure = adminDong("1162069500", "관악구", "신림동", 37.4842, 126.9295);
            AdminDong arrival = adminDong("1168064000", "강남구", "역삼1동", 37.4998, 127.0364);

            given(adminDongRepository.getByAdminDongCode("1162069500")).willReturn(departure);
            given(adminDongRepository.getByAdminDongCode("1168064000")).willReturn(arrival);
            given(
                            skCarRouteClient.route(
                                    eq("신림동"),
                                    eq(126.9295),
                                    eq(37.4842),
                                    eq("역삼1동"),
                                    eq(127.0364),
                                    eq(37.4998)))
                    .willReturn(parseJson(SAMPLE_TMAP_CAR_RESPONSE));

            CarPathRequest request =
                    CarPathRequest.builder()
                            .departureDongCode("1162069500")
                            .arrivalDongCode("1168064000")
                            .build();

            CarPathResponse response = pathService.searchCar(request);

            assertThat(response.getDepartureDong().getAdminDongCode()).isEqualTo("1162069500");
            assertThat(response.getArrivalDong().getAdminDongCode()).isEqualTo("1168064000");
            assertThat(response.getTotalDistance()).isEqualTo(12500);
            assertThat(response.getTotalTime()).isEqualTo(18);
        }

        @Test
        @DisplayName("출발 코드와 도착 코드가 같으면 TRANSIT_PATH_SAME_DONG")
        void search_sameDongCode_throws() {
            CarPathRequest request =
                    CarPathRequest.builder()
                            .departureDongCode("1162069500")
                            .arrivalDongCode("1162069500")
                            .build();

            assertThatThrownBy(() -> pathService.searchCar(request))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TRANSIT_PATH_SAME_DONG);
        }
    }

    private AdminDong adminDong(
            String code, String districtName, String dongName, double lat, double lng) {
        return new AdminDong(
                new AdminDongCsvRow(
                        code, "서울특별시", districtName, dongName, lat, lng, "20260325", ""));
    }

    private Map<String, Object> parseJson(String json) throws IOException {
        @SuppressWarnings("unchecked")
        Map<String, Object> map = OBJECT_MAPPER.readValue(json, Map.class);
        return map;
    }

    private static final String SAMPLE_ODSAY_RESPONSE =
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
                        "lane": {"name": "수도권 2호선"},
                        "stationCount": 1,
                        "startX": 126.902682,
                        "startY": 37.534863,
                        "startName": "당산",
                        "endX": 126.914543,
                        "endY": 37.549942,
                        "endName": "합정"
                      }
                    ],
                    "info": {
                      "mapObj": "2:2:237:238",
                      "payment": 1250,
                      "busTransitCount": 0,
                      "subwayTransitCount": 1,
                      "totalTime": 9,
                      "totalWalk": 0,
                      "totalWalkTime": -1
                    }
                  }
                ],
                "searchType": 0
              }
            }
            """;

    private static final String SAMPLE_TMAP_PEDESTRIAN_RESPONSE =
            """
            {
              "type": "FeatureCollection",
              "features": [
                {
                  "type": "Feature",
                  "geometry": {"type": "Point", "coordinates": [126.9295, 37.4842]},
                  "properties": {
                    "totalDistance": 1240,
                    "totalTime": 1067,
                    "pointType": "SP"
                  }
                }
              ]
            }
            """;

    private static final String SAMPLE_TMAP_CAR_RESPONSE =
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
