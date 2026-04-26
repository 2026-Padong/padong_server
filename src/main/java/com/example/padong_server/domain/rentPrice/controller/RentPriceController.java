package com.example.padong_server.domain.rentPrice.controller;

import com.example.padong_server.domain.rentPrice.dto.request.RentPriceSummaryRequest;
import com.example.padong_server.domain.rentPrice.dto.response.AdminDongRentPriceDetailResponse;
import com.example.padong_server.domain.rentPrice.dto.response.AdminDongRentPriceSummaryResponse;
import com.example.padong_server.domain.rentPrice.dto.response.RentPriceImportResponse;
import com.example.padong_server.domain.rentPrice.dto.response.RentPriceErrorResponse;
import com.example.padong_server.domain.rentPrice.service.RentPriceDataImportService;
import com.example.padong_server.domain.rentPrice.service.RentPriceService;
import java.util.List;
import lombok.RequiredArgsConstructor;
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
public class RentPriceController {

    private final RentPriceService rentPriceService;
    private final RentPriceDataImportService rentPriceDataImportService;

    @PostMapping("/data")
    public ResponseEntity<RentPriceImportResponse> importRentPriceData() {
        return ResponseEntity.ok(rentPriceDataImportService.importData());
    }

    @PostMapping("/summary")
    public ResponseEntity<List<AdminDongRentPriceSummaryResponse>> getAdminDongSummaries(
            @RequestBody RentPriceSummaryRequest request
    ) {
        return ResponseEntity.ok(rentPriceService.getSummaries(
                request.adminDongCodes(),
                request.buildingTypeLabel(),
                request.tradeTypeLabel()
        ));
    }

    @GetMapping("/{adminDongCode}")
    public ResponseEntity<AdminDongRentPriceDetailResponse> getAdminDongDetail(
            @PathVariable String adminDongCode
    ) {
        return ResponseEntity.ok(rentPriceService.getDetail(adminDongCode));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<RentPriceErrorResponse> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(new RentPriceErrorResponse(exception.getMessage()));
    }
}
