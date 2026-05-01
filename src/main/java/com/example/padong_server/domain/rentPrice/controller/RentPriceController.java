package com.example.padong_server.domain.rentPrice.controller;

import com.example.padong_server.domain.rentPrice.dto.request.RentPriceSummaryRequest;
import com.example.padong_server.domain.rentPrice.dto.response.AdminDongRentPriceDetailResponse;
import com.example.padong_server.domain.rentPrice.dto.response.AdminDongRentPriceSummaryResponse;
import com.example.padong_server.domain.rentPrice.dto.response.RentPriceImportResponse;
import com.example.padong_server.domain.rentPrice.dto.response.RentPriceErrorResponse;
import com.example.padong_server.domain.rentPrice.service.RentPriceDataImportService;
import com.example.padong_server.domain.rentPrice.service.RentPriceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/rent-price")
@Tag(name = "RentPrice", description = "행정동 기준 주거 실거래가 통계 API")
public class RentPriceController {

    private final RentPriceService rentPriceService;
    private final RentPriceDataImportService rentPriceDataImportService;

    @Operation(
            summary = "주거 실거래가 데이터 적재",
            description = """
                    최신 rentPrice 적재 API입니다.
                    원본 CSV의 법정동코드는 적재 과정에서 DongMapping으로 행정동에 매핑하고,
                    DB에는 행정동 + 건물유형 기준 rent_price 통계만 저장합니다.
                    가격 단위는 만원입니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "행정동 기준 rent_price 적재 성공",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RentPriceImportResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "savedStatCount": 1692,
                                              "sourceRowCount": 1497726,
                                              "saleRowCount": 231263,
                                              "jeonseRowCount": 495772,
                                              "monthlyRentRowCount": 756246,
                                              "skippedRowCount": 14445
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "법정동-행정동 매핑 누락 등 적재 검증 실패",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RentPriceErrorResponse.class),
                            examples = @ExampleObject(value = "{\"message\":\"행정동 매핑이 없는 법정동 코드입니다: 1111010100\"}")
                    )
            )
    })
    @PostMapping("/data")
    public ResponseEntity<RentPriceImportResponse> importRentPriceData() {
        return ResponseEntity.ok(rentPriceDataImportService.importData());
    }

    @Operation(
            summary = "행정동별 주거비 요약 조회",
            description = """
                    행정동 코드 목록을 받아 선택한 건물유형/거래유형의 대표 주거비를 반환합니다.
                    buildingTypeLabel은 아파트, 오피스텔, 연립다세대, 단독다가구 또는 enum code를 받을 수 있고,
                    미선택 시 단독다가구가 기본값입니다.
                    tradeTypeLabel은 매매, 전세, 월세 또는 enum code를 받을 수 있고, 미선택 시 월세가 기본값입니다.
                    응답 금액 단위는 만원입니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "행정동별 주거비 요약 조회 성공",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = AdminDongRentPriceSummaryResponse.class)),
                            examples = @ExampleObject(
                                    value = """
                                            [
                                              {
                                                "adminDongCode": "1168064000",
                                                "periodLabel": "최근 2년 기준",
                                                "buildingType": {
                                                  "buildingTypeCode": "APARTMENT",
                                                  "buildingTypeLabel": "아파트"
                                                },
                                                "tradeType": {
                                                  "tradeTypeCode": "MONTHLY_RENT",
                                                  "tradeTypeLabel": "월세"
                                                },
                                                "sale": {
                                                  "amount": null
                                                },
                                                "jeonse": {
                                                  "amount": null
                                                },
                                                "monthlyRent": {
                                                  "deposit": 5000,
                                                  "monthlyRent": 180
                                                }
                                              }
                                            ]
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "행정동 코드 누락, 존재하지 않는 행정동, 지원하지 않는 건물/거래유형",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RentPriceErrorResponse.class),
                            examples = @ExampleObject(value = "{\"message\":\"존재하지 않는 행정동 코드입니다: 9999999999\"}")
                    )
            )
    })
    @PostMapping("/summary")
    public ResponseEntity<List<AdminDongRentPriceSummaryResponse>> getAdminDongSummaries(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "행정동 코드 목록과 선택 필터. 가격 응답 단위는 만원입니다.",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RentPriceSummaryRequest.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "adminDongCodes": ["1168064000", "1156054000"],
                                              "buildingTypeLabel": "아파트",
                                              "tradeTypeLabel": "월세"
                                            }
                                            """
                            )
                    )
            )
            @RequestBody RentPriceSummaryRequest request
    ) {
        return ResponseEntity.ok(rentPriceService.getSummaries(
                request.adminDongCodes(),
                request.buildingTypeLabel(),
                request.tradeTypeLabel()
        ));
    }

    @Operation(
            summary = "행정동 주거비 상세 조회",
            description = """
                    단일 행정동의 건물유형별 매매/전세/월세 대표 금액을 모두 반환합니다.
                    최신 구조 기준으로 rent_price는 행정동 + 건물유형 단위이며, 응답 금액 단위는 만원입니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "행정동 주거비 상세 조회 성공",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AdminDongRentPriceDetailResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "adminDongCode": "1168064000",
                                              "periodLabel": "최근 2년 기준",
                                              "contractPeriodStart": "2024-04-18",
                                              "contractPeriodEnd": "2026-04-17",
                                              "excludedCancelledSales": true,
                                              "dominantBuildingType": {
                                                "buildingTypeCode": "APARTMENT",
                                                "buildingTypeLabel": "아파트"
                                              },
                                              "buildingTypes": [
                                                {
                                                  "buildingType": {
                                                    "buildingTypeCode": "APARTMENT",
                                                    "buildingTypeLabel": "아파트"
                                                  },
                                                  "sale": {
                                                    "amount": 250000
                                                  },
                                                  "jeonse": {
                                                    "amount": 120000
                                                  },
                                                  "monthlyRent": {
                                                    "deposit": 5000,
                                                    "monthlyRent": 180
                                                  }
                                                }
                                              ]
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "행정동 코드 누락 또는 존재하지 않는 행정동",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RentPriceErrorResponse.class),
                            examples = @ExampleObject(value = "{\"message\":\"존재하지 않는 행정동 코드입니다: 9999999999\"}")
                    )
            )
    })
    @GetMapping("/{adminDongCode}")
    public ResponseEntity<AdminDongRentPriceDetailResponse> getAdminDongDetail(
            @Parameter(description = "행정동 코드", example = "1168064000")
            @PathVariable String adminDongCode
    ) {
        return ResponseEntity.ok(rentPriceService.getDetail(adminDongCode));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<RentPriceErrorResponse> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(new RentPriceErrorResponse(exception.getMessage()));
    }
}
