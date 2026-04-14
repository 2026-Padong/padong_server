package com.example.padong_server.domain.rentPrice.controller;

import com.example.padongbe.domain.rentPrice.dto.response.RentPriceDto;
import com.example.padongbe.domain.rentPrice.service.RentPriceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/rent-price")
@Tag(name = "RentPrice", description = "전월세가 관련 API")
public class RentPriceController {
    private final RentPriceService rentPriceService;

    @PostMapping("/data")
    @Operation(summary = "전월세가 데이터 저장")
    public ResponseEntity<String> uploadRentPriceData() {
        rentPriceService.uploadRentPriceData();
        return ResponseEntity.ok("Success to save rent price data");
    }

    @GetMapping("/{buildingType}/{adminDongCode}")
    @Operation(summary = "행정동 코드로 건물유형별 전월세가 조회")
    @Parameter(name = "buildingType", description = "건물 유형 (예: apartment, officetel, villa)")
    @Parameter(name = "adminDongCode", description = "행정동 코드 10자리 (예:1141069000)")
    public ResponseEntity<RentPriceDto> getRentPriceByAdminDongCode(@PathVariable String buildingType, @PathVariable String adminDongCode) {
        return ResponseEntity.ok(rentPriceService.getRentPriceByAdminDongCode(adminDongCode, buildingType));
    }

}
