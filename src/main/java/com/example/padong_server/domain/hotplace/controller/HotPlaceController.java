package com.example.padong_server.domain.hotplace.controller;

import com.example.padong_server.domain.hotplace.service.HotPlaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/hot-places")
public class HotPlaceController {

    private final HotPlaceService hotPlaceService;

    @PostMapping("/data")
    public ResponseEntity<String> uploadHotPlaceData() {
        int savedCount = hotPlaceService.uploadHotPlaceData();
        return ResponseEntity.ok("Success to save hot place data: " + savedCount);
    }
}
