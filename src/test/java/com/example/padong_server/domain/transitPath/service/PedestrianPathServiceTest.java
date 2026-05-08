package com.example.padong_server.domain.transitPath.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.example.padong_server.domain.dongne.dto.AdminDongCsvRow;
import com.example.padong_server.domain.dongne.entity.AdminDong;
import com.example.padong_server.domain.dongne.repository.AdminDongRepository;
import com.example.padong_server.domain.transitPath.dto.request.PedestrianPathRequest;
import com.example.padong_server.domain.transitPath.dto.response.PedestrianPathResponse;
import com.example.padong_server.global.client.sk.SkPedestrianRouteClient;
import com.example.padong_server.global.exception.CustomException;
import com.example.padong_server.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class PedestrianPathServiceTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mock
    private AdminDongRepository adminDongRepository;

    @Mock
    private SkPedestrianRouteClient skPedestrianRouteClient;

    @InjectMocks
    private PedestrianPathService pedestrianPathService;

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
                .willReturn(parseJson(SAMPLE_TMAP_RESPONSE));

        PedestrianPathRequest request =
                PedestrianPathRequest.builder()
                        .departureDongCode("1162069500")
                        .arrivalDongCode("1168064000")
                        .build();

        PedestrianPathResponse response = pedestrianPathService.search(request);

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

        assertThatThrownBy(() -> pedestrianPathService.search(request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TRANSIT_PATH_SAME_DONG);
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

    private static final String SAMPLE_TMAP_RESPONSE =
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
}
