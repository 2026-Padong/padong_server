package com.example.padong_server.domain.boundary.service;

import com.example.padong_server.domain.boundary.dto.BoundaryResponse;
import com.example.padong_server.domain.dongne.entity.LegalDong;
import com.example.padong_server.domain.dongne.service.DongneService;
import com.example.padong_server.global.ResponseDTO;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@EnableConfigurationProperties(BoundaryProperties.class)
public class BoundaryService {

    private final DongneService dongneService;
    private final BoundaryProperties properties;
    private final Gson gson = new Gson();

    public ResponseDTO<BoundaryResponse> getBoundary(String geocode) {
        LegalDong legalDong = dongneService.findLegalDongByAdminCode(geocode);
        if (legalDong == null) {
            return ResponseDTO.res(HttpStatus.OK, "법정동으로 변환 실패");
        }

        validateProperties();

        geocode = legalDong.getLegalDongCode();
        log.info("{} {}", geocode, legalDong.getLegalDongName());

        String legalCode = geocode.substring(0, 8);
        log.info(legalCode);

        String jsonString = WebClient.builder()
                .baseUrl(properties.baseUrl())
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/req/data")
                        .queryParam("service", "data")
                        .queryParam("request", "GetFeature")
                        .queryParam("data", "LT_C_ADEMD_INFO")
                        .queryParam("key", properties.serviceKey())
                        .queryParam("domain", properties.domain())
                        .queryParam("attrFilter", "emdCd:=:" + legalCode)
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .block();

        JsonObject root = gson.fromJson(jsonString, JsonObject.class);

        JsonArray coordinates = root.getAsJsonObject("response")
                .getAsJsonObject("result")
                .getAsJsonObject("featureCollection")
                .getAsJsonArray("features")
                .get(0)
                .getAsJsonObject()
                .getAsJsonObject("geometry")
                .getAsJsonArray("coordinates")
                .get(0)
                .getAsJsonArray()
                .get(0)
                .getAsJsonArray();

        List<List<Double>> points = new ArrayList<>();
        for (JsonElement coord : coordinates) {
            JsonArray pair = coord.getAsJsonArray();
            double lon = pair.get(0).getAsDouble();
            double lat = pair.get(1).getAsDouble();

            List<Double> point = new ArrayList<>();
            point.add(lon);
            point.add(lat);
            points.add(point);
        }

        BoundaryResponse response = new BoundaryResponse(points);
        return ResponseDTO.res(HttpStatus.OK, "조회 성공", response);
    }

    private void validateProperties() {
        if (!StringUtils.hasText(properties.baseUrl())
                || !StringUtils.hasText(properties.serviceKey())
                || !StringUtils.hasText(properties.domain())) {
            throw new IllegalStateException("boundary-api 설정이 비어 있습니다.");
        }
    }
}
