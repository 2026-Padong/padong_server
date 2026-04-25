package com.example.padong_server.domain.subway.controller;

import com.example.padong_server.domain.subway.util.SubwayCsvLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/subway")
public class SubwayController {

    private final SubwayCsvLoader subwayCsvLoader;

    @GetMapping("/load")
    public ResponseEntity<String> loadCsv() {
        subwayCsvLoader.loadCsv();
        return ResponseEntity.ok("CSV 데이터 적재 완료");
    }
}
