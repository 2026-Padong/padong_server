package com.example.padong_server.domain.dongne.controller;

import com.example.padong_server.domain.dongne.service.DongneService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/dongne")
public class DongneController {

    private final DongneService dongneService;

    @PostMapping("/data")
    public ResponseEntity<String> addDongneData() {
        dongneService.addDongneDate();
        return ResponseEntity.ok("동네 데이터 추가 완료");
    }

    /*
    @PostMapping("/data/safety-grade")
    public ResponseEntity<String> addDongneSafetyGradeData() {
        dongneService.addDongneSafetyGradeData();
        return ResponseEntity.ok("동네 안전등급 데이터 추가 완료");
    }

    @GetMapping("/detail")
    public ResponseEntity<ResponseDTO<DetailResponse>> getDongneDetail(
            @RequestParam String arrivalCode,
            @RequestParam String departureCode
    ) {
        return ResponseEntity.ok(dongneDetailService.getDongneDetail(arrivalCode, departureCode));
    }
    */
}
