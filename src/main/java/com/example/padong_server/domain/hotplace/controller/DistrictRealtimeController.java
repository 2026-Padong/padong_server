package com.example.padong_server.domain.hotplace.controller;

import com.example.padong_server.domain.hotplace.dto.DistrictRealtimeResponse;
import com.example.padong_server.domain.hotplace.service.HotplaceRealtimeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "District Realtime", description = "자치구 기준 실시간 도시데이터 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/realtime/districts")
public class DistrictRealtimeController {

    private final HotplaceRealtimeService hotplaceRealtimeService;

    @Operation(
            summary = "자치구 실시간 데이터 조회",
            description = "query parameter guName에 해당하는 자치구의 핫플레이스 목록을 조회하고, 대표 핫플레이스의 날씨 요약과 전체 핫플레이스 혼잡도 목록을 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "실시간 데이터 조회 성공",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DistrictRealtimeResponse.class),
                            examples = @ExampleObject(
                                    name = "용산구 조회 예시",
                                    value = """
                                            {
                                              "districtName": "용산구",
                                              "selectedAreaName": "국립중앙박물관",
                                              "summary": {
                                                "weatherStatus": "맑음",
                                                "temperature": "21.3",
                                                "sensibleTemperature": "22.0",
                                                "humidity": "55%",
                                                "pm10Status": "좋음",
                                                "pm10": "18.0",
                                                "precipitationProbability": "10%"
                                              },
                                              "hotplaces": [
                                                {
                                                  "areaName": "국립중앙박물관",
                                                  "thumbnail": "https://example.com/museum.jpg",
                                                  "roadAddress": "서울 용산구 서빙고로 137",
                                                  "minPopulation": "12000",
                                                  "maxPopulation": "18000",
                                                  "congestionLevel": "여유",
                                                  "dominantAgeGroup": "20대",
                                                  "dominantAgeRate": "31.2%",
                                                  "roadTrafficStatus": "원활",
                                                  "roadTrafficSpeed": "42.5"
                                                }
                                              ]
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "404", description = "해당 자치구의 핫플레이스를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서울시 실시간 API 호출 또는 내부 처리 실패")
    })
    @GetMapping
    public ResponseEntity<DistrictRealtimeResponse> getDistrictRealtime(
            @Parameter(description = "조회할 자치구 이름", example = "용산구")
            @RequestParam String guName
    ) {
        return ResponseEntity.ok(hotplaceRealtimeService.getDistrictRealtime(guName));
    }
}
