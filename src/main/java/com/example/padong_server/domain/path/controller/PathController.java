package com.example.padong_server.domain.path.controller;

import com.example.padong_server.domain.path.dto.request.CarPathRequest;
import com.example.padong_server.domain.path.dto.request.PathAllRequest;
import com.example.padong_server.domain.path.dto.request.PedestrianPathRequest;
import com.example.padong_server.domain.path.dto.request.TransitPathRequest;
import com.example.padong_server.domain.path.dto.response.CarPathResponse;
import com.example.padong_server.domain.path.dto.response.PathAllResponse;
import com.example.padong_server.domain.path.dto.response.PedestrianPathResponse;
import com.example.padong_server.domain.path.dto.response.TransitPathResponse;
import com.example.padong_server.domain.path.service.PathService;
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
@RequestMapping("/path")
@Tag(name = "길찾기", description = "행정동 A → 행정동 B 경로 조회 API (대중교통 / 보행자 / 자동차)")
@RequiredArgsConstructor
public class PathController {

    private final PathService pathService;

    @GetMapping
    @Operation(
            summary = "행정동 A → 행정동 B 통합 길찾기 (대중교통/보행자/자동차)",
            description =
                    """
                    행정동 코드 2개를 입력받아 대중교통/보행자/자동차 3개 모드의
                    소요시간(분)과 거리(m)를 한번에 반환한다.
                    각 모드는 DB record (1일 EXPIRATION) 우선 조회 후 miss 시 외부 API 호출.
                    """)
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "통합 길찾기 조회 성공",
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
                                                          "message": "통합 길찾기 조회 성공",
                                                          "data": {
                                                            "departureDong": {
                                                              "adminDongCode": "1162069500",
                                                              "address": "서울특별시 관악구 신림동"
                                                            },
                                                            "arrivalDong": {
                                                              "adminDongCode": "1168064000",
                                                              "address": "서울특별시 강남구 역삼1동"
                                                            },
                                                            "paths": {
                                                              "transit":    { "totalTime": 42,  "totalDistance": 10800, "source": "ODSAY" },
                                                              "pedestrian": { "totalTime": 132, "totalDistance": 10400, "source": "TMAP" },
                                                              "car":        { "totalTime": 28,  "totalDistance": 11500, "source": "TMAP" }
                                                            }
                                                          }
                                                        }
                                                        """))),
        @ApiResponse(responseCode = "400", description = "필수값 누락 / 출발=도착 / 700m 이내"),
        @ApiResponse(responseCode = "404", description = "행정동 미존재 또는 결과 없음"),
        @ApiResponse(responseCode = "502", description = "외부 API 호출 실패"),
        @ApiResponse(responseCode = "503", description = "외부 API 키 미설정")
    })
    public ResponseEntity<ResponseDTO<PathAllResponse>> searchAllPath(
            @Valid @ParameterObject PathAllRequest request) {
        PathAllResponse response = pathService.searchAll(request);
        return ResponseEntity.ok(
                ResponseDTO.res(HttpStatus.OK, "통합 길찾기 조회 성공", response));
    }

    @GetMapping("/transit")
    @Operation(
            summary = "행정동 A → 행정동 B 대중교통 길찾기",
            description =
                    """
                    행정동 코드 2개를 입력받아 ODsay 대중교통 길찾기 API를 호출하여
                    총 소요시간(분)과 총 거리(m)를 반환한다.
                    캐시(1일 TTL) 우선 조회 후 miss 시 외부 API 호출.
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
                                                            "totalTime": 42,
                                                            "totalDistance": 10800
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
        TransitPathResponse response = pathService.searchTransit(request);
        return ResponseEntity.ok(
                ResponseDTO.res(HttpStatus.OK, "대중교통 길찾기 조회 성공", response));
    }

    @GetMapping("/pedestrian")
    @Operation(
            summary = "행정동 A → 행정동 B 보행자 경로 조회",
            description =
                    """
                    행정동 코드 2개를 입력받아 SK TMAP 보행자 경로 API를 호출하여
                    총 소요시간(분)과 총 거리(m)를 반환한다.
                    좌표는 AdminDong.latitude/longitude 를 사용한다.
                    """)
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "보행자 경로 조회 성공",
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
                                                          "message": "보행자 경로 조회 성공",
                                                          "data": {
                                                            "departureDong": {
                                                              "adminDongCode": "1162069500",
                                                              "address": "서울특별시 관악구 신림동"
                                                            },
                                                            "arrivalDong": {
                                                              "adminDongCode": "1168064000",
                                                              "address": "서울특별시 강남구 역삼1동"
                                                            },
                                                            "totalTime": 132,
                                                            "totalDistance": 10400
                                                          }
                                                        }
                                                        """))),
        @ApiResponse(responseCode = "400", description = "필수값 누락 / 출발=도착"),
        @ApiResponse(responseCode = "404", description = "행정동 미존재 또는 결과 없음"),
        @ApiResponse(responseCode = "502", description = "TMAP API 호출 실패"),
        @ApiResponse(responseCode = "503", description = "TMAP API 키 미설정")
    })
    public ResponseEntity<ResponseDTO<PedestrianPathResponse>> searchPedestrianPath(
            @Valid @ParameterObject PedestrianPathRequest request) {
        PedestrianPathResponse response = pathService.searchPedestrian(request);
        return ResponseEntity.ok(
                ResponseDTO.res(HttpStatus.OK, "보행자 경로 조회 성공", response));
    }

    @GetMapping("/car")
    @Operation(
            summary = "행정동 A → 행정동 B 자동차 경로 조회",
            description =
                    """
                    행정동 코드 2개를 입력받아 SK TMAP 자동차 경로 안내 API를 호출하여
                    총 소요시간(분)과 총 거리(m)를 반환한다.
                    좌표는 AdminDong.latitude/longitude 를 사용한다.
                    """)
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "자동차 경로 조회 성공",
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
                                                          "message": "자동차 경로 조회 성공",
                                                          "data": {
                                                            "departureDong": {
                                                              "adminDongCode": "1162069500",
                                                              "address": "서울특별시 관악구 신림동"
                                                            },
                                                            "arrivalDong": {
                                                              "adminDongCode": "1168064000",
                                                              "address": "서울특별시 강남구 역삼1동"
                                                            },
                                                            "totalTime": 28,
                                                            "totalDistance": 11500
                                                          }
                                                        }
                                                        """))),
        @ApiResponse(responseCode = "400", description = "필수값 누락 / 출발=도착"),
        @ApiResponse(responseCode = "404", description = "행정동 미존재 또는 결과 없음"),
        @ApiResponse(responseCode = "502", description = "TMAP API 호출 실패"),
        @ApiResponse(responseCode = "503", description = "TMAP API 키 미설정")
    })
    public ResponseEntity<ResponseDTO<CarPathResponse>> searchCarPath(
            @Valid @ParameterObject CarPathRequest request) {
        CarPathResponse response = pathService.searchCar(request);
        return ResponseEntity.ok(
                ResponseDTO.res(HttpStatus.OK, "자동차 경로 조회 성공", response));
    }
}
