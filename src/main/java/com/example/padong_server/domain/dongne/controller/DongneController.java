package com.example.padong_server.domain.dongne.controller;

import com.example.padong_server.domain.dongne.dto.DongneDetailResponse;
import com.example.padong_server.domain.dongne.service.DongneDetailService;
import com.example.padong_server.domain.dongne.service.DongneService;
import com.example.padong_server.domain.dongneLike.dto.DongneLikeToggleResponse;
import com.example.padong_server.domain.dongneLike.service.DongneLikeService;
import com.example.padong_server.domain.oauth.entity.CustomUserDetails;
import com.example.padong_server.global.ResponseDTO;
import com.example.padong_server.global.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/dongne")
@Tag(name = "Dongne", description = "서울시 행정동/법정동 기반 데이터 관리 API")
public class DongneController {

    private final DongneService dongneService;
    private final DongneDetailService dongneDetailService;
    private final DongneLikeService dongneLikeService;

    @Operation(
            summary = "동네 기초 데이터 적재",
            description = """
                    classpath의 서울시 행정동, 법정동, 행정동-법정동 매핑 CSV를 읽어
                    admin_dong, legal_dong, dong_mapping 데이터를 적재합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "동네 기초 데이터 적재 성공",
                    content = @Content(
                            mediaType = MediaType.TEXT_PLAIN_VALUE,
                            schema = @Schema(implementation = String.class),
                            examples = @ExampleObject(value = "동네 데이터 추가 완료")
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "CSV 파싱 실패 또는 데이터 적재 실패",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "code": "INTERNAL_SERVER_ERROR",
                                              "message": "서버 내부 오류가 발생했습니다."
                                            }
                                            """
                            )
                    )
            )
    })
    @PostMapping("/data")
    public ResponseEntity<String> addDongneData() {
        dongneService.addDongneDate();
        return ResponseEntity.ok("동네 데이터 추가 완료");
    }

    @GetMapping("/detail")
    @Operation(
            summary = "동네 상세 조회",
            description = """
                    선택한 거주지 후보(adminDongCode)와 직장 위치(arrivalAdminDongCode)를 기준으로
                    동네 요약, 인구, 전월세, 생활이동, 안전지수, 통합 길찾기 정보를 반환합니다.
                    arrivalAdminDongCode가 없거나 출발과 도착이 같으면 paths와 mobility는 null/empty입니다.
                    안전지수는 선택한 동이 속한 구(districtName) 기준으로 조회합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "동네 상세 조회 성공",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ResponseDTO.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "statusCode": "200",
                                      "message": "동네 상세 조회 성공",
                                      "data": {
                                        "departureDong": {
                                          "adminDongCode": "1162069500",
                                          "cityName": "서울특별시",
                                          "districtName": "관악구",
                                          "adminDongName": "신림동",
                                          "address": "서울특별시 관악구 신림동",
                                          "latitude": 37.4842,
                                          "longitude": 126.9295
                                        },
                                        "arrivalDong": {
                                          "adminDongCode": "1168064000",
                                          "cityName": "서울특별시",
                                          "districtName": "강남구",
                                          "adminDongName": "역삼1동",
                                          "address": "서울특별시 강남구 역삼1동",
                                          "latitude": 37.4998,
                                          "longitude": 127.0364
                                        },
                                        "mobility": {
                                          "totalMobility": 1240.5,
                                          "avgTime": 47.8,
                                          "startMonth": "202601",
                                          "endMonth": "202603"
                                        },
                                        "totalPopulation": 41250,
                                        "density": 23150.4,
                                        "safety": {
                                          "overallScore": "C",
                                          "lifeSafetyGrade": "C",
                                          "trafficAccidentGrade": "B",
                                          "fireGrade": "C",
                                          "crimeGrade": "D"
                                        },
                                        "rentPrice": {
                                          "adminDongCode": "1162069500",
                                          "periodLabel": "최근 2년 기준",
                                          "contractPeriodStart": "2024-04-18",
                                          "contractPeriodEnd": "2026-04-17",
                                          "excludedCancelledSales": true
                                        },
                                        "paths": {
                                          "transit": {
                                            "totalTime": 42,
                                            "totalDistance": 10800,
                                            "source": "ODSAY"
                                          },
                                          "pedestrian": {
                                            "totalTime": 132,
                                            "totalDistance": 10400,
                                            "source": "TMAP"
                                          },
                                          "car": {
                                            "totalTime": 28,
                                            "totalDistance": 11500,
                                            "source": "TMAP"
                                          }
                                        },
                                        "likeCount": 23,
                                        "likedByCurrentUser": true
                                      }
                                    }
                                    """)
                    )
            )
    })
    public ResponseEntity<ResponseDTO<DongneDetailResponse>> getDongneDetail(
            @RequestParam String adminDongCode,
            @RequestParam(required = false) String arrivalAdminDongCode,
            @RequestParam(required = false) Long userId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long resolvedUserId = userDetails != null ? userDetails.getUserId() : userId;
        return ResponseEntity.ok(
                dongneDetailService.getDetail(adminDongCode, arrivalAdminDongCode, resolvedUserId));
    }

    @PostMapping("/likes")
    @Operation(summary = "동네 좋아요 토글")
    public ResponseEntity<ResponseDTO<DongneLikeToggleResponse>> toggleDongneLike(
            @RequestParam String adminDongCode,
            @RequestParam(required = false) Long userId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long resolvedUserId = userDetails != null ? userDetails.getUserId() : userId;
        DongneLikeToggleResponse response =
                dongneLikeService.toggleLike(adminDongCode, resolvedUserId);
        return ResponseEntity.ok(
                ResponseDTO.res(HttpStatus.OK, "동네 좋아요 상태가 변경되었습니다.", response));
    }
}
