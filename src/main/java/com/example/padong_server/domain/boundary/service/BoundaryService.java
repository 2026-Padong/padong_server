package com.example.padong_server.domain.boundary.service;

import com.example.padongbe.domain.boundary.dto.BoundaryResponse;
import com.example.padongbe.domain.dongne.entity.LegalDong;
import com.example.padongbe.domain.dongne.service.DongneService;
import com.example.padongbe.global.ResponseDTO;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BoundaryService {

  private final DongneService dongneService;
  private final WebClient boundaryClient = WebClient.builder()
      .baseUrl("http://api.vworld.kr")
      .build();
  private final String boundaryServiceKey= "E1D96B74-5EBD-37FC-9FCE-78648323050F";

  private final Gson gson = new Gson();

  public ResponseDTO<BoundaryResponse> getBoundary(String geocode) {

    LegalDong legalDong= dongneService.findLegalDongByAdminCode(geocode);
    if (legalDong== null) return ResponseDTO.res(HttpStatus.OK, "법정동으로 변환 실패");
    else geocode= legalDong.getLegalDongCode();

    log.info(geocode+" "+legalDong.getLegalDongName());

    String legalCode = geocode.substring(0,8);

    log.info(legalCode);

    String jsonString = boundaryClient.get()
        .uri(uriBuilder -> uriBuilder
            .path("/req/data")
            .queryParam("service", "data")
            .queryParam("request", "GetFeature")
            .queryParam("data", "LT_C_ADEMD_INFO")
            .queryParam("key", boundaryServiceKey)
            .queryParam("domain", "www.padong.site")
            .queryParam("attrFilter", "emdCd:=:" + legalCode)
            .build())
        .retrieve()
        .bodyToMono(String.class)
        .block(); // (주의: block()은 동기적으로 응답 대기)

    // 이제 Gson으로 파싱
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
      double lon = pair.get(0).getAsDouble(); // 경도
      double lat = pair.get(1).getAsDouble(); // 위도

      List<Double> point = new ArrayList<>();
      point.add(lon);
      point.add(lat);
      points.add(point);

    }
    BoundaryResponse response= new BoundaryResponse(points);
    return ResponseDTO.res(HttpStatus.OK, "조회 성공", response);
  }


}
