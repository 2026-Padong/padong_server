package com.example.padong_server.domain.activityMobility.controller;

import com.example.padong_server.domain.activityMobility.dto.CommonDepartureMobilityResponse;
import com.example.padong_server.domain.activityMobility.dto.MobilityFilterRequest;
import com.example.padong_server.domain.activityMobility.dto.MobilityResponse;
import com.example.padong_server.domain.activityMobility.dto.MobilitySimpleResponse;
import com.example.padong_server.domain.activityMobility.dto.MultiMobilityResponse;
import com.example.padong_server.domain.activityMobility.entity.Mobility;
import com.example.padong_server.domain.activityMobility.service.MobilityImportService;
import com.example.padong_server.domain.activityMobility.service.MobilityService;
import com.example.padong_server.domain.activityMobility.service.SafetyIndexService;
import com.example.padong_server.global.PageResponse;
import com.example.padong_server.global.ResponseDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/mobility")
@Tag(name = "생활이동", description = "서울시 생활이동 데이터 관리 API")
@RequiredArgsConstructor
public class MobilityController {
    private final MobilityService mobilityService;
    private final MobilityImportService mobilityImportService;
    private final SafetyIndexService safetyIndexService;

    @GetMapping("/arrival/multi")
    @Operation(
            summary = "여러 행정동 코드로 공통 생활이동 많은 순 검색",
            description =
                    """
                    조회 대상: 여러 도착 행정동의 공통 출발 후보 행정동
                    정렬 기준: totalMobility 합산 내림차순
                    arrivalDongCodes 전달 방식: 반복 query parameter
                    자치구 필터 기준: 출발 후보 행정동 districtName
                    시간 필터 기준: 각 도착지별 평균 이동시간
                    시간 필터 파라미터: minEachAvgTime, maxEachAvgTime
                    가격 필터 단위: 만원
                    가격 필터 조건: contractType별 가격 필드 단독 사용
                    기본 표시 주거/가격: MONTHLY_RENT + DETACHED_MULTIFAMILY

                    contractType별 가격 필터:
                    - MONTHLY_RENT: minMonthlyDeposit, maxMonthlyDeposit, minMonthlyRent, maxMonthlyRent
                    - JEONSE: minJeonseDeposit, maxJeonseDeposit
                    - SALE: minSalePrice, maxSalePrice

                    공통 선택 필터:
                    - departureDistrictNames: 출발 후보 행정동의 자치구 이름
                    - houseType: APARTMENT, OFFICETEL, ROW_MULTIFAMILY, DETACHED_MULTIFAMILY
                    """)
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "다중 행정동 생활이동 조회 성공",
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
                                                          "message": "다중 행정동 생활이동 많은 순 조회 성공",
                                                          "data": {
                                                            "content": [
                                                              {
                                                                "departureDong": {
                                                                  "adminDongCode": "1162069500",
                                                                  "address": "서울특별시 관악구 신림동"
                                                                },
                                                                "totalMobility": 400.58,
                                                                "rentPrice": {
                                                                  "buildingType": {
                                                                    "buildingTypeCode": "OFFICETEL",
                                                                    "buildingTypeLabel": "오피스텔"
                                                                  },
                                                                  "tradeType": {
                                                                    "tradeTypeCode": "MONTHLY_RENT",
                                                                    "tradeTypeLabel": "월세"
                                                                  },
                                                                  "price": {
                                                                    "deposit": 3000,
                                                                    "monthlyRent": 70
                                                                  }
                                                                }
                                                              }
                                                            ],
                                                            "page": 0,
                                                            "size": 10,
                                                            "totalElements": 120,
                                                            "totalPages": 12,
                                                            "first": true,
                                                            "last": false,
                                                            "hasNext": true,
                                                            "hasPrevious": false
                                                          }
                                                        }
                                                        """))),
        @ApiResponse(responseCode = "400", description = "필터 값 또는 행정동 코드 개수 오류"),
        @ApiResponse(responseCode = "404", description = "공통 생활이동 데이터 없음")
    })
    public ResponseEntity<ResponseDTO<PageResponse<CommonDepartureMobilityResponse>>>
            searchByArrivalDongCodes(
                    @Parameter(
                                    description =
                                            "도착 행정동 코드 목록, 반복 query parameter, 예:"
                                                + " arrivalDongCodes=1168064000&arrivalDongCodes=1156054000",
                                    example = "1168064000")
                            @RequestParam
                    List<String> arrivalDongCodes,
                    @Parameter(description = "페이지 번호, 0부터 시작", example = "0")
                            @RequestParam(defaultValue = "0")
                            int page,
                    @Parameter(description = "페이지 크기", example = "10")
                            @RequestParam(defaultValue = "10")
                            int size,
                    @Valid @ParameterObject MobilityFilterRequest filterRequest) {
        Pageable pageable = PageRequest.of(page, size);
        ResponseDTO<PageResponse<CommonDepartureMobilityResponse>> response =
                mobilityService.searchByArrivalDongCodes(arrivalDongCodes, pageable, filterRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/arrival/{adminDongCode}")
    @Operation(
            summary = "행정동 코드로 생활이동 많은 순 검색",
            description =
                    """
                    조회 대상: 단일 도착 행정동의 출발 후보 행정동
                    정렬 기준: totalMobility 내림차순
                    자치구 필터 기준: 출발 후보 행정동 districtName
                    시간 필터 기준: 평균 이동시간
                    시간 필터 파라미터: minAvgTime, maxAvgTime
                    가격 필터 단위: 만원
                    가격 필터 조건: contractType별 가격 필드 단독 사용
                    기본 표시 주거/가격: MONTHLY_RENT + DETACHED_MULTIFAMILY

                    contractType별 가격 필터:
                    - MONTHLY_RENT: minMonthlyDeposit, maxMonthlyDeposit, minMonthlyRent, maxMonthlyRent
                    - JEONSE: minJeonseDeposit, maxJeonseDeposit
                    - SALE: minSalePrice, maxSalePrice

                    공통 선택 필터:
                    - departureDistrictNames: 출발 후보 행정동의 자치구 이름
                    - houseType: APARTMENT, OFFICETEL, ROW_MULTIFAMILY, DETACHED_MULTIFAMILY
                    """)
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "단일 행정동 생활이동 조회 성공",
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
                                                          "message": "생활이동 많은 순 조회 성공",
                                                          "data": {
                                                            "content": [
                                                              {
                                                                "departureDong": {
                                                                  "adminDongCode": "1162069500",
                                                                  "address": "서울특별시 관악구 신림동"
                                                                },
                                                                "totalMobility": 477.07,
                                                                "avgTime": 42.7,
                                                                "rentPrice": {
                                                                  "buildingType": {
                                                                    "buildingTypeCode": "OFFICETEL",
                                                                    "buildingTypeLabel": "오피스텔"
                                                                  },
                                                                  "tradeType": {
                                                                    "tradeTypeCode": "MONTHLY_RENT",
                                                                    "tradeTypeLabel": "월세"
                                                                  },
                                                                  "price": {
                                                                    "deposit": 3000,
                                                                    "monthlyRent": 70
                                                                  }
                                                                }
                                                              }
                                                            ],
                                                            "page": 0,
                                                            "size": 10,
                                                            "totalElements": 424,
                                                            "totalPages": 43,
                                                            "first": true,
                                                            "last": false,
                                                            "hasNext": true,
                                                            "hasPrevious": false
                                                          }
                                                        }
                                                        """))),
        @ApiResponse(responseCode = "400", description = "필터 값 오류"),
        @ApiResponse(responseCode = "404", description = "생활이동 데이터 없음")
    })
    public ResponseEntity<ResponseDTO<PageResponse<MobilitySimpleResponse>>>
            searchByArrivalDongCode(
                    @Parameter(description = "도착 행정동 코드", example = "1168064000") @PathVariable
                            String adminDongCode,
                    @Parameter(description = "페이지 번호, 0부터 시작", example = "0")
                            @RequestParam(defaultValue = "0")
                            int page,
                    @Parameter(description = "페이지 크기", example = "10")
                            @RequestParam(defaultValue = "10")
                            int size,
                    @Valid @ParameterObject MobilityFilterRequest filterRequest) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Direction.DESC, "totalMobility"));
        ResponseDTO<PageResponse<MobilitySimpleResponse>> response =
                mobilityService.searchByArrivalDongCode(adminDongCode, pageable, filterRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/data")
    @Operation(summary = "서울시 생활이동 데이터 저장")
    public ResponseEntity<ResponseDTO<Void>> fetchData() {
        String message = mobilityImportService.importData();
        return ResponseEntity.ok(ResponseDTO.res(HttpStatus.OK, message));
    }

    @PostMapping("/safety/data")
    @Operation(summary = "서울시 안전지수 데이터 적재")
    public ResponseEntity<ResponseDTO<Void>> fetchSafetyData() {
        String message = safetyIndexService.importData();
        return ResponseEntity.ok(ResponseDTO.res(HttpStatus.OK, message));
    }

    // 사용 중단된 주소 기반 레거시 API
    @Deprecated
    @GetMapping("/address")
    @Operation(summary = "행정동 주소로 검색", deprecated = true)
    @Parameter(name = "address", description = "행정동 주소", example = "서울특별시 서대문구 남가좌1동")
    public ResponseEntity<ResponseDTO<List<MobilityResponse>>> searchByAddress(
            @RequestParam String address, @RequestParam(defaultValue = "0") int page) {
        Pageable pageable = PageRequest.of(page, 10, Sort.by(Direction.DESC, "totalMobility"));
        ResponseDTO<List<MobilityResponse>> response =
                mobilityService.searchByAddress(address, pageable);
        return ResponseEntity.ok(response);
    }

    @Deprecated
    @GetMapping("/address/multi")
    @Operation(summary = "두 개의 행정동 주소로 검색", deprecated = true)
    public ResponseEntity<ResponseDTO<MultiMobilityResponse>> searchByTwoAddresses(
            @RequestBody Mobility mobility,
            @RequestParam String address1,
            @RequestParam String address2,
            @RequestParam int page) {
        return ResponseEntity.ok(mobilityService.searchByTwoAddresses(address1, address2, page));
    }
}
