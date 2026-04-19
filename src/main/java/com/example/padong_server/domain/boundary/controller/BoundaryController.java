package com.example.padong_server.domain.boundary.controller;

import com.example.padongbe.domain.boundary.dto.BoundaryResponse;
import com.example.padongbe.domain.boundary.service.BoundaryService;
import com.example.padongbe.global.ResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@RequestMapping("/boundary")
@Tag(name = "Boundary", description = "행정동 경계 데이터 관리 API")
public class BoundaryController {
    private final BoundaryService boundaryService;

    @GetMapping("/{geocode}")
    @Operation(summary = "행정동 경계 조회")
    public ResponseEntity<ResponseDTO<BoundaryResponse>> getBoundary(@PathVariable String geocode) {
        return ResponseEntity.ok(boundaryService.getBoundary(geocode));
    }

}
