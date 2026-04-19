package com.example.padong_server.domain.dongne.controller;

import com.example.padongbe.domain.dongne.dto.DetailResponse;
import com.example.padongbe.domain.dongne.service.DongneDetailService;
import com.example.padongbe.domain.dongne.service.DongneService;
import com.example.padongbe.global.ResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/dongne")
@Tag(name = "Dongne", description = "동네 관련 API")
public class DongneController {

    private final DongneService dongneService;
    private final DongneDetailService dongneDetailService;

    @PostMapping("/data")
    @Operation(summary = "동네 데이터 추가")
    public ResponseEntity<String> addDongneData(){
        dongneService.addDongneDate();
        return ResponseEntity.ok("동네 데이터 추가 완료");
    }

    @PostMapping("/data/safety-grade")
    @Operation(summary = "동네 안전등급 데이터 추가")
    public ResponseEntity<String> addDongneSafetyGradeData(){
        dongneService.addDongneSafetyGradeData();
        return ResponseEntity.ok("동네 안전등급 데이터 추가 완료");
    }

    @GetMapping("/detail")
    @Operation(summary = "행정동 상세조회")
    public ResponseEntity<ResponseDTO<DetailResponse>> getDongneDetail(@RequestParam String arrivalCode,@RequestParam String departureCode){
        return ResponseEntity.ok(dongneDetailService.getDongneDetail(arrivalCode,departureCode));
    }
}
