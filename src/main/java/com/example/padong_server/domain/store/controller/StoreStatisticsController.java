package com.example.padong_server.domain.store.controller;

import com.example.padong_server.domain.store.service.StoreStatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/store-statistics")
public class StoreStatisticsController {

    private final StoreStatisticsService storeStatisticsService;

    @PostMapping("/data")
    public ResponseEntity<String> uploadStoreStatisticsData() {
        int savedCount = storeStatisticsService.uploadStoreStatisticsData();
        return ResponseEntity.ok("Success to save store statistics data: " + savedCount);
    }
}
