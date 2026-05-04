package com.example.padong_server.domain.hotplace.controller;

import com.example.padong_server.domain.hotplace.dto.DistrictRealtimeResponse;
import com.example.padong_server.domain.hotplace.service.HotplaceRealtimeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "자치구 실시간", description = "자치구 기준 실시간 핫플레이스 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/realtime/districts")
public class DistrictRealtimeController {

    private final HotplaceRealtimeService hotplaceRealtimeService;

    @Operation(
            summary = "자치구 실시간 데이터 조회",
            description = "guName에 해당하는 자치구의 핫플레이스 목록과 대표 날씨 요약, 각 핫플레이스 카드 데이터를 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "실시간 데이터 조회 성공"),
            @ApiResponse(responseCode = "404", description = "해당 자치구의 핫플레이스를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서울시 실시간 API 호출 또는 내부 처리 실패")
    })
    @GetMapping
    public ResponseEntity<DistrictRealtimeResponse> getDistrictRealtime(
            @Parameter(description = "조회할 자치구 이름", example = "종로구")
            @RequestParam String guName
    ) {
        return ResponseEntity.ok(hotplaceRealtimeService.getDistrictRealtime(guName));
    }
}
