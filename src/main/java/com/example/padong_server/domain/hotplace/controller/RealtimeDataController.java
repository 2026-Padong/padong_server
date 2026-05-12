package com.example.padong_server.domain.hotplace.controller;

import com.example.padong_server.domain.hotplace.dto.DistrictSummaryResponse;
import com.example.padong_server.domain.hotplace.dto.HotplaceRealtimeItem;
import com.example.padong_server.domain.hotplace.service.HotPlaceService;
import com.example.padong_server.domain.hotplace.service.HotplaceRealtimeService;
import com.example.padong_server.global.PageResponse;
import com.example.padong_server.global.ResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "실시간 데이터", description = "핫플레이스 CSV 적재 + 자치구 단위 실시간 조회 (summary / hotplaces 분리)")
@RestController
@RequiredArgsConstructor
@RequestMapping("/realtime")
public class RealtimeDataController {

    private static final int MAX_HOTPLACE_PAGE_SIZE = 10;
    private static final int DEFAULT_HOTPLACE_PAGE_SIZE = 3;

    private final HotPlaceService hotPlaceService;
    private final HotplaceRealtimeService hotplaceRealtimeService;

    @Operation(summary = "핫플레이스 CSV 적재", description = "리소스 CSV 를 읽어 hot_place 테이블에 저장합니다.")
    @PostMapping("/data")
    public ResponseEntity<String> uploadHotPlaceData() {
        int savedCount = hotPlaceService.uploadHotPlaceData();
        return ResponseEntity.ok("Success to save hot place data: " + savedCount);
    }

    @Operation(
            summary = "자치구 실시간 요약",
            description = "자치구의 날씨·대기 등 요약만 반환. 핫플레이스 리스트는 별도 endpoint 사용.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "요약 조회 성공"),
            @ApiResponse(responseCode = "404", description = "해당 자치구의 핫플레이스를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서울시 실시간 API 호출 또는 내부 처리 실패")
    })
    @GetMapping("/districts/{guName}/summary")
    public ResponseEntity<ResponseDTO<DistrictSummaryResponse>> getDistrictSummary(
            @Parameter(description = "자치구 한글명", example = "종로구") @PathVariable String guName) {
        DistrictSummaryResponse response = hotplaceRealtimeService.getDistrictSummary(guName);
        return ResponseEntity.ok(ResponseDTO.res(HttpStatus.OK, "자치구 실시간 요약 조회 성공", response));
    }

    @Operation(
            summary = "자치구 핫플레이스 (offset 페이징)",
            description =
                    "자치구의 핫플레이스 카드 리스트를 page/size offset 으로 페이징. "
                            + "응답은 공통 PageResponse — 1 2 3 ... 형태 페이지 네비게이션 가능. "
                            + "size 기본 " + DEFAULT_HOTPLACE_PAGE_SIZE + ", 최대 " + MAX_HOTPLACE_PAGE_SIZE + ".")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "해당 자치구의 핫플레이스를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서울시 실시간 API 호출 또는 내부 처리 실패")
    })
    @GetMapping("/districts/{guName}/hotplaces")
    public ResponseEntity<ResponseDTO<PageResponse<HotplaceRealtimeItem>>> getDistrictHotplaces(
            @Parameter(description = "자치구 한글명", example = "종로구") @PathVariable String guName,
            @Parameter(description = "0 부터 시작하는 페이지 번호", example = "0")
                    @RequestParam(required = false, defaultValue = "0") int page,
            @Parameter(description = "한 페이지 크기 (기본 " + DEFAULT_HOTPLACE_PAGE_SIZE
                    + ", 최대 " + MAX_HOTPLACE_PAGE_SIZE + ")", example = "3")
                    @RequestParam(required = false, defaultValue = "" + DEFAULT_HOTPLACE_PAGE_SIZE) int size) {
        int pageNum = Math.max(page, 0);
        int capped = Math.min(Math.max(size, 1), MAX_HOTPLACE_PAGE_SIZE);

        // page 안에 들어가는 POI 만 fetch — 자치구 전체 (~14개) 일괄 fetch 회피
        HotplaceRealtimeService.DistrictHotplacesPage pageData =
                hotplaceRealtimeService.getDistrictHotplacesPage(guName, pageNum, capped);

        return ResponseEntity.ok(
                ResponseDTO.res(
                        HttpStatus.OK,
                        "핫플레이스 조회 성공",
                        PageResponse.of(pageData.content(), pageNum, capped, pageData.totalElements())));
    }
}
