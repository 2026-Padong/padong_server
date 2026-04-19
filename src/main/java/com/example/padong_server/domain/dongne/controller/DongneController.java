package com.example.padong_server.domain.dongne.controller;

import com.example.padong_server.domain.dongne.dto.DongneImportResult;
import com.example.padong_server.domain.dongne.service.DongneImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/dongne")
public class DongneController {

    private final DongneImportService dongneImportService;

    @PostMapping("/data")
    public ResponseEntity<DongneImportResult> importDongneData() {
        return ResponseEntity.ok(dongneImportService.importData());
    }
}
