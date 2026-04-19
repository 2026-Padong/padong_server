package com.example.padong_server.domian.population.controller;

import com.example.padongbe.domain.population.service.PopulationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/population")
@Tag(name = "Population", description = "동네별 인구수 관련 API")
public class PopulationController {

    private final PopulationService populationService;

    @PostMapping("/data")
    @Operation(summary = "동네별 인구수 데이터 저장")
    public ResponseEntity<String> uploadPopulationData() {
        populationService.uploadPopulationData();
        return ResponseEntity.ok("Success to save population data");
    }

    @PostMapping("/density/data")
    @Operation(summary = "동네별 인구밀도 데이터 저장")
    public ResponseEntity<String> uploadDensityData() {
        populationService.uploadDensityData();
        return ResponseEntity.ok("Success to save Density data");
    }

//    @GetMapping("/{dongneCode}")
//    @Operation(summary = "동네별 인구밀도 조회")
//    public ResponseEntity<ResponseDTO<PopulationDataDto>> getPopulationByDongneCode(@PathVariable String dongneCode) {
//        return ResponseEntity.ok(populationService.getPopulationByAdmin(dongneCode));
//    }


}
