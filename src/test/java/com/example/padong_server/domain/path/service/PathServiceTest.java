package com.example.padong_server.domain.path.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.padong_server.domain.dongne.dto.AdminDongCsvRow;
import com.example.padong_server.domain.dongne.entity.AdminDong;
import com.example.padong_server.domain.dongne.repository.AdminDongRepository;
import com.example.padong_server.domain.path.dto.request.CarPathRequest;
import com.example.padong_server.domain.path.dto.request.PathAllRequest;
import com.example.padong_server.domain.path.dto.request.PedestrianPathRequest;
import com.example.padong_server.domain.path.dto.request.TransitPathRequest;
import com.example.padong_server.domain.path.dto.response.CarPathResponse;
import com.example.padong_server.domain.path.dto.response.PathAllResponse;
import com.example.padong_server.domain.path.dto.response.PedestrianPathResponse;
import com.example.padong_server.domain.path.dto.response.TransitPathResponse;
import com.example.padong_server.domain.path.entity.PathMode;
import com.example.padong_server.domain.path.entity.PathRecord;
import com.example.padong_server.domain.path.entity.PathSource;
import com.example.padong_server.domain.path.repository.PathRecordRepository;
import com.example.padong_server.global.client.google.GoogleRoutesClient;
import com.example.padong_server.global.client.odsay.OdsayClient;
import com.example.padong_server.global.client.sk.SkCarRouteClient;
import com.example.padong_server.global.client.sk.SkPedestrianRouteClient;
import com.example.padong_server.global.exception.CustomException;
import com.example.padong_server.global.exception.ErrorCode;
import tools.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

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

    @Mock
    private GoogleRoutesClient googleRoutesClient;

    @Mock
    private PathRecordRepository pathRecordRepository;

    @InjectMocks
    private PathService pathService;

    @Nested
    @DisplayName("대중교통 (searchTransit)")
    class Transit {

        @Test
        @DisplayName("DB hit (만료 전): 외부 API 호출 없이 응답")
        void search_freshRecord_skipsExternalCall() {
            stubAdminDongs();
            given(pathRecordRepository.findByModeAndDepartureDongCodeAndArrivalDongCode(
                            PathMode.TRANSIT, "1162069500", "1168064000"))
                    .willReturn(Optional.of(record(PathMode.TRANSIT, 9, 2000, LocalDateTime.now())));

            TransitPathResponse response =
                    pathService.searchTransit(
                            TransitPathRequest.builder()
                                    .departureDongCode("1162069500")
                                    .arrivalDongCode("1168064000")
                                    .build());

            assertThat(response.getTotalTime()).isEqualTo(9);
            assertThat(response.getTotalDistance()).isEqualTo(2000);
            verify(odsayClient, never()).searchPubTransPath(
                    any(Double.class), any(Double.class), any(Double.class), any(Double.class),
                    any(), any());
            verify(pathRecordRepository, never()).upsert(
                    anyString(), anyString(), anyString(), anyInt(), anyInt(), anyString(), any());
        }

        @Test
        @DisplayName("DB miss: 외부 API 호출 + UPSERT")
        void search_missingRecord_callsExternalAndUpserts() throws IOException {
            stubAdminDongs();
            given(pathRecordRepository.findByModeAndDepartureDongCodeAndArrivalDongCode(
                            PathMode.TRANSIT, "1162069500", "1168064000"))
                    .willReturn(Optional.empty());
            given(odsayClient.searchPubTransPath(
                            eq(126.9295), eq(37.4842), eq(127.0364), eq(37.4998),
                            eq((Integer) null), eq((Integer) null)))
                    .willReturn(parseJson(SAMPLE_ODSAY_RESPONSE));

            TransitPathResponse response =
                    pathService.searchTransit(
                            TransitPathRequest.builder()
                                    .departureDongCode("1162069500")
                                    .arrivalDongCode("1168064000")
                                    .build());

            assertThat(response.getTotalTime()).isEqualTo(9);
            assertThat(response.getTotalDistance()).isEqualTo(2000);
            verify(pathRecordRepository).upsert(
                    eq("TRANSIT"), eq("1162069500"), eq("1168064000"),
                    eq(9), eq(2000), eq("ODSAY"), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("DB hit but 만료: 외부 API 호출 + UPSERT")
        void search_expiredRecord_refreshes() throws IOException {
            stubAdminDongs();
            given(pathRecordRepository.findByModeAndDepartureDongCodeAndArrivalDongCode(
                            PathMode.TRANSIT, "1162069500", "1168064000"))
                    .willReturn(
                            Optional.of(record(
                                    PathMode.TRANSIT, 9, 2000,
                                    LocalDateTime.now().minusDays(2)))); // 만료
            given(odsayClient.searchPubTransPath(
                            any(Double.class), any(Double.class), any(Double.class),
                            any(Double.class), any(), any()))
                    .willReturn(parseJson(SAMPLE_ODSAY_RESPONSE));

            pathService.searchTransit(
                    TransitPathRequest.builder()
                            .departureDongCode("1162069500")
                            .arrivalDongCode("1168064000")
                            .build());

            verify(pathRecordRepository).upsert(
                    eq("TRANSIT"), anyString(), anyString(), anyInt(), anyInt(), anyString(),
                    any(LocalDateTime.class));
        }

        @Test
        @DisplayName("출발 코드와 도착 코드가 같으면 TRANSIT_PATH_SAME_DONG")
        void search_sameDongCode_throws() {
            assertThatThrownBy(
                            () -> pathService.searchTransit(
                                    TransitPathRequest.builder()
                                            .departureDongCode("1162069500")
                                            .arrivalDongCode("1162069500")
                                            .build()))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TRANSIT_PATH_SAME_DONG);
        }

        @Test
        @DisplayName("ODsay 실패 → Google fallback 호출")
        void search_odsayFails_fallsBackToGoogle() {
            stubAdminDongs();
            given(pathRecordRepository.findByModeAndDepartureDongCodeAndArrivalDongCode(
                            PathMode.TRANSIT, "1162069500", "1168064000"))
                    .willReturn(Optional.empty());
            given(odsayClient.searchPubTransPath(
                            any(Double.class), any(Double.class), any(Double.class),
                            any(Double.class), any(), any()))
                    .willThrow(new CustomException(ErrorCode.ODSAY_NO_RESULT));
            given(googleRoutesClient.route(
                            eq(GoogleRoutesClient.TravelMode.TRANSIT),
                            eq(37.4842), eq(126.9295),
                            eq(37.4998), eq(127.0364)))
                    .willReturn(new com.example.padong_server.domain.path.dto.internal.PathSummary(
                            25, 8500, PathSource.GOOGLE));

            TransitPathResponse response =
                    pathService.searchTransit(
                            TransitPathRequest.builder()
                                    .departureDongCode("1162069500")
                                    .arrivalDongCode("1168064000")
                                    .build());

            assertThat(response.getTotalTime()).isEqualTo(25);
            assertThat(response.getTotalDistance()).isEqualTo(8500);
            verify(pathRecordRepository).upsert(
                    eq("TRANSIT"), eq("1162069500"), eq("1168064000"),
                    eq(25), eq(8500), eq("GOOGLE"), any(LocalDateTime.class));
        }
    }

    @Nested
    @DisplayName("보행자 (searchPedestrian)")
    class Pedestrian {

        @Test
        @DisplayName("DB hit (만료 전): 외부 API 호출 없이 응답")
        void search_freshRecord_skipsExternalCall() {
            stubAdminDongs();
            given(pathRecordRepository.findByModeAndDepartureDongCodeAndArrivalDongCode(
                            PathMode.PEDESTRIAN, "1162069500", "1168064000"))
                    .willReturn(Optional.of(record(PathMode.PEDESTRIAN, 18, 1240, LocalDateTime.now())));

            PedestrianPathResponse response =
                    pathService.searchPedestrian(
                            PedestrianPathRequest.builder()
                                    .departureDongCode("1162069500")
                                    .arrivalDongCode("1168064000")
                                    .build());

            assertThat(response.getTotalTime()).isEqualTo(18);
            assertThat(response.getTotalDistance()).isEqualTo(1240);
            verify(skPedestrianRouteClient, never()).route(
                    any(), any(Double.class), any(Double.class),
                    any(), any(Double.class), any(Double.class));
        }

        @Test
        @DisplayName("DB miss: 외부 API 호출 + UPSERT")
        void search_missingRecord_callsExternalAndUpserts() throws IOException {
            stubAdminDongs();
            given(pathRecordRepository.findByModeAndDepartureDongCodeAndArrivalDongCode(
                            PathMode.PEDESTRIAN, "1162069500", "1168064000"))
                    .willReturn(Optional.empty());
            given(skPedestrianRouteClient.route(
                            eq("신림동"), eq(126.9295), eq(37.4842),
                            eq("역삼1동"), eq(127.0364), eq(37.4998)))
                    .willReturn(parseJson(SAMPLE_TMAP_PEDESTRIAN_RESPONSE));

            pathService.searchPedestrian(
                    PedestrianPathRequest.builder()
                            .departureDongCode("1162069500")
                            .arrivalDongCode("1168064000")
                            .build());

            verify(pathRecordRepository).upsert(
                    eq("PEDESTRIAN"), eq("1162069500"), eq("1168064000"),
                    eq(18), eq(1240), eq("TMAP"), any(LocalDateTime.class));
        }
    }

    @Nested
    @DisplayName("자동차 (searchCar)")
    class Car {

        @Test
        @DisplayName("DB hit (만료 전): 외부 API 호출 없이 응답")
        void search_freshRecord_skipsExternalCall() {
            stubAdminDongs();
            given(pathRecordRepository.findByModeAndDepartureDongCodeAndArrivalDongCode(
                            PathMode.CAR, "1162069500", "1168064000"))
                    .willReturn(Optional.of(record(PathMode.CAR, 18, 12500, LocalDateTime.now())));

            CarPathResponse response =
                    pathService.searchCar(
                            CarPathRequest.builder()
                                    .departureDongCode("1162069500")
                                    .arrivalDongCode("1168064000")
                                    .build());

            assertThat(response.getTotalTime()).isEqualTo(18);
            assertThat(response.getTotalDistance()).isEqualTo(12500);
            verify(skCarRouteClient, never()).route(
                    any(), any(Double.class), any(Double.class),
                    any(), any(Double.class), any(Double.class));
        }

        @Test
        @DisplayName("DB miss: 외부 API 호출 + UPSERT")
        void search_missingRecord_callsExternalAndUpserts() throws IOException {
            stubAdminDongs();
            given(pathRecordRepository.findByModeAndDepartureDongCodeAndArrivalDongCode(
                            PathMode.CAR, "1162069500", "1168064000"))
                    .willReturn(Optional.empty());
            given(skCarRouteClient.route(
                            eq("신림동"), eq(126.9295), eq(37.4842),
                            eq("역삼1동"), eq(127.0364), eq(37.4998)))
                    .willReturn(parseJson(SAMPLE_TMAP_CAR_RESPONSE));

            pathService.searchCar(
                    CarPathRequest.builder()
                            .departureDongCode("1162069500")
                            .arrivalDongCode("1168064000")
                            .build());

            verify(pathRecordRepository).upsert(
                    eq("CAR"), eq("1162069500"), eq("1168064000"),
                    eq(18), eq(12500), eq("TMAP"), any(LocalDateTime.class));
        }
    }

    @Nested
    @DisplayName("통합 (searchAll)")
    class All {

        @Test
        @DisplayName("3개 모드 모두 DB hit: 외부 API 호출 0회")
        void search_allFresh_skipsAllExternalCalls() {
            stubAdminDongs();
            given(pathRecordRepository.findByModeAndDepartureDongCodeAndArrivalDongCode(
                            PathMode.TRANSIT, "1162069500", "1168064000"))
                    .willReturn(Optional.of(record(PathMode.TRANSIT, 9, 2000, LocalDateTime.now())));
            given(pathRecordRepository.findByModeAndDepartureDongCodeAndArrivalDongCode(
                            PathMode.PEDESTRIAN, "1162069500", "1168064000"))
                    .willReturn(Optional.of(record(PathMode.PEDESTRIAN, 18, 1240, LocalDateTime.now())));
            given(pathRecordRepository.findByModeAndDepartureDongCodeAndArrivalDongCode(
                            PathMode.CAR, "1162069500", "1168064000"))
                    .willReturn(Optional.of(record(PathMode.CAR, 18, 12500, LocalDateTime.now())));

            PathAllResponse response =
                    pathService.searchAll(
                            PathAllRequest.builder()
                                    .departureDongCode("1162069500")
                                    .arrivalDongCode("1168064000")
                                    .build());

            assertThat(response.getDepartureDong().getAdminDongCode()).isEqualTo("1162069500");
            assertThat(response.getArrivalDong().getAdminDongCode()).isEqualTo("1168064000");
            assertThat(response.getPaths().getTransit().totalTime()).isEqualTo(9);
            assertThat(response.getPaths().getTransit().totalDistance()).isEqualTo(2000);
            assertThat(response.getPaths().getPedestrian().totalTime()).isEqualTo(18);
            assertThat(response.getPaths().getPedestrian().totalDistance()).isEqualTo(1240);
            assertThat(response.getPaths().getCar().totalTime()).isEqualTo(18);
            assertThat(response.getPaths().getCar().totalDistance()).isEqualTo(12500);

            verify(odsayClient, never()).searchPubTransPath(
                    any(Double.class), any(Double.class), any(Double.class), any(Double.class),
                    any(), any());
            verify(skPedestrianRouteClient, never()).route(
                    any(), any(Double.class), any(Double.class),
                    any(), any(Double.class), any(Double.class));
            verify(skCarRouteClient, never()).route(
                    any(), any(Double.class), any(Double.class),
                    any(), any(Double.class), any(Double.class));
            verify(pathRecordRepository, never()).upsert(
                    anyString(), anyString(), anyString(), anyInt(), anyInt(), anyString(), any());
        }

        @Test
        @DisplayName("모두 DB miss: 3개 모드 모두 외부 API 호출 + 각각 UPSERT")
        void search_allMiss_callsAllExternalAndUpserts() throws IOException {
            stubAdminDongs();
            given(pathRecordRepository.findByModeAndDepartureDongCodeAndArrivalDongCode(
                            any(), eq("1162069500"), eq("1168064000")))
                    .willReturn(Optional.empty());
            given(odsayClient.searchPubTransPath(
                            any(Double.class), any(Double.class), any(Double.class),
                            any(Double.class), any(), any()))
                    .willReturn(parseJson(SAMPLE_ODSAY_RESPONSE));
            given(skPedestrianRouteClient.route(
                            any(), any(Double.class), any(Double.class),
                            any(), any(Double.class), any(Double.class)))
                    .willReturn(parseJson(SAMPLE_TMAP_PEDESTRIAN_RESPONSE));
            given(skCarRouteClient.route(
                            any(), any(Double.class), any(Double.class),
                            any(), any(Double.class), any(Double.class)))
                    .willReturn(parseJson(SAMPLE_TMAP_CAR_RESPONSE));

            PathAllResponse response =
                    pathService.searchAll(
                            PathAllRequest.builder()
                                    .departureDongCode("1162069500")
                                    .arrivalDongCode("1168064000")
                                    .build());

            assertThat(response.getPaths().getTransit().totalTime()).isEqualTo(9);
            assertThat(response.getPaths().getPedestrian().totalTime()).isEqualTo(18);
            assertThat(response.getPaths().getCar().totalTime()).isEqualTo(18);

            verify(pathRecordRepository).upsert(
                    eq("TRANSIT"), eq("1162069500"), eq("1168064000"),
                    eq(9), eq(2000), eq("ODSAY"), any(LocalDateTime.class));
            verify(pathRecordRepository).upsert(
                    eq("PEDESTRIAN"), eq("1162069500"), eq("1168064000"),
                    eq(18), eq(1240), eq("TMAP"), any(LocalDateTime.class));
            verify(pathRecordRepository).upsert(
                    eq("CAR"), eq("1162069500"), eq("1168064000"),
                    eq(18), eq(12500), eq("TMAP"), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("출발=도착이면 TRANSIT_PATH_SAME_DONG")
        void search_sameDongCode_throws() {
            assertThatThrownBy(
                            () -> pathService.searchAll(
                                    PathAllRequest.builder()
                                            .departureDongCode("1162069500")
                                            .arrivalDongCode("1162069500")
                                            .build()))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TRANSIT_PATH_SAME_DONG);
        }
    }

    private void stubAdminDongs() {
        AdminDong departure = adminDong("1162069500", "관악구", "신림동", 37.4842, 126.9295);
        AdminDong arrival = adminDong("1168064000", "강남구", "역삼1동", 37.4998, 127.0364);
        given(adminDongRepository.getByAdminDongCode("1162069500")).willReturn(departure);
        given(adminDongRepository.getByAdminDongCode("1168064000")).willReturn(arrival);
    }

    private AdminDong adminDong(
            String code, String districtName, String dongName, double lat, double lng) {
        return new AdminDong(
                new AdminDongCsvRow(
                        code, "서울특별시", districtName, dongName, lat, lng, "20260325", ""));
    }

    private PathRecord record(PathMode mode, int totalTime, int totalDistance, LocalDateTime updatedAt) {
        PathSource source = mode == PathMode.TRANSIT ? PathSource.ODSAY : PathSource.TMAP;
        return PathRecord.builder()
                .mode(mode)
                .departureDongCode("1162069500")
                .arrivalDongCode("1168064000")
                .totalTime(totalTime)
                .totalDistance(totalDistance)
                .source(source)
                .createdAt(updatedAt)
                .updatedAt(updatedAt)
                .build();
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
                    "info": {
                      "totalTime": 9,
                      "totalDistance": 2000
                    }
                  }
                ]
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
