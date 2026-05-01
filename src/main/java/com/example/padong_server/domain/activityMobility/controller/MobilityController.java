package com.example.padong_server.domain.activityMobility.controller;

import com.example.padong_server.domain.activityMobility.dto.MobilityResponse;
import com.example.padong_server.domain.activityMobility.dto.MultiMobilityResponse;
import com.example.padong_server.domain.activityMobility.entity.Mobility;
import com.example.padong_server.domain.activityMobility.service.MobilityImportService;
import com.example.padong_server.domain.activityMobility.service.MobilityService;
import com.example.padong_server.global.ResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/mobility")
@Tag(name = "Mobility", description = "서울시 생활이동 데이터 관리 API")
@RequiredArgsConstructor
public class MobilityController {
    private final MobilityService mobilityService;
    private final MobilityImportService mobilityImportService;

    @GetMapping("/address")
    @Operation(summary = "행정동 주소로 검색")
    @Parameter(name = "address", description = "행정동 주소", example = "서울특별시 서대문구 남가좌1동")
    public ResponseEntity<ResponseDTO<List<MobilityResponse>>> searchByAddress(@RequestParam String address,
        @RequestParam(defaultValue = "0") int page
        ) {
        Pageable pageable = PageRequest.of(page, 10, Sort.by(Direction.DESC, "totalMobility"));
        ResponseDTO<List<MobilityResponse>> response = mobilityService.searchByAddress(address, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/address/multi")
    @Operation(summary = "두 개의 행정동 주소로 검색")
    public ResponseEntity<ResponseDTO<MultiMobilityResponse>> searchByTwoAddresses( @RequestBody Mobility mobility,
            @RequestParam String address1,
            @RequestParam String address2,
            @RequestParam int page
    )
    {
        return ResponseEntity.ok(mobilityService.searchByTwoAddresses(address1, address2, page));
    }

    @PostMapping("/data")
    @Operation(summary = "서울시 생활이동 데이터 저장")
    public ResponseEntity<ResponseDTO<Void>> fetchData() {
        String message = mobilityImportService.importData();
        return ResponseEntity.ok(ResponseDTO.res(HttpStatus.OK, message));
    }

}
