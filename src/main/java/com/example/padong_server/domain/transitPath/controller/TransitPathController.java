package com.example.padong_server.domain.transitPath.controller;

import com.example.padong_server.domain.transitPath.dto.TransitPathRequest;
import com.example.padong_server.domain.transitPath.dto.TransitPathResponse;
import com.example.padong_server.domain.transitPath.service.TransitPathService;
import com.example.padong_server.global.ResponseDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/transit-path")
@Tag(name = "대중교통 길찾기", description = "ODsay 기반 행정동 A → 행정동 B 대중교통 경로 조회 API")
@RequiredArgsConstructor
public class TransitPathController {

    private final TransitPathService transitPathService;

    @GetMapping
    @Operation(
            summary = "행정동 A → 행정동 B 대중교통 길찾기",
            description =
                    """
                    행정동 코드 2개를 입력받아 ODsay 대중교통 길찾기 API를 호출하여
                    추천 경로를 반환한다.
                    좌표는 AdminDong.latitude/longitude 를 사용한다.
                    도시내(SearchType=0)만 우선 지원한다.
                    """)
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "대중교통 길찾기 조회 성공",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ResponseDTO.class),
                                examples =
                                        @ExampleObject(
                                                value =
                                                        """
                                                        {
                                                          "statusCode": "200",
                                                          "message": "대중교통 길찾기 조회 성공",
                                                          "data": {
                                                            "departureDong": {
                                                              "adminDongCode": "1162069500",
                                                              "address": "서울특별시 관악구 신림동"
                                                            },
                                                            "arrivalDong": {
                                                              "adminDongCode": "1168064000",
                                                              "address": "서울특별시 강남구 역삼1동"
                                                            },
                                                            "searchType": 0,
                                                            "pathCount": 1,
                                                            "paths": [
                                                              {
                                                                "pathType": 1,
                                                                "totalTime": 9,
                                                                "totalWalk": 0,
                                                                "totalWalkTime": null,
                                                                "payment": 1250,
                                                                "transferCount": 0,
                                                                "mapObj": "2:2:237:238",
                                                                "subPaths": [
                                                                  {
                                                                    "trafficType": 1,
                                                                    "distance": 2000,
                                                                    "sectionTime": 9,
                                                                    "startName": "당산",
                                                                    "endName": "합정",
                                                                    "startCoord": { "x": 126.902682, "y": 37.534863 },
                                                                    "endCoord":   { "x": 126.914543, "y": 37.549942 },
                                                                    "laneName": "수도권 2호선",
                                                                    "stationCount": 1
                                                                  }
                                                                ]
                                                              }
                                                            ]
                                                          }
                                                        }
                                                        """))),
        @ApiResponse(responseCode = "400", description = "필수값 누락 / 출발=도착 / 700m 이내"),
        @ApiResponse(responseCode = "404", description = "행정동 미존재 또는 정류장/결과 없음"),
        @ApiResponse(responseCode = "502", description = "ODsay API 호출 실패"),
        @ApiResponse(responseCode = "503", description = "ODsay API 키 미설정")
    })
    public ResponseEntity<ResponseDTO<TransitPathResponse>> searchTransitPath(
            @Valid @ParameterObject TransitPathRequest request) {
        TransitPathResponse response = transitPathService.search(request);
        return ResponseEntity.ok(
                ResponseDTO.res(HttpStatus.OK, "대중교통 길찾기 조회 성공", response));
    }
}
