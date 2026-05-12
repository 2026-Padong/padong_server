package com.example.padong_server.domain.dongne.controller;

import com.example.padong_server.domain.dongne.dto.DongSuggestionListResponse;
import com.example.padong_server.domain.dongne.service.DongneService;
import com.example.padong_server.global.ResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "행정동 검색", description = "가게 검색·출퇴근 동네 찾기에서 사용하는 행정동 자동완성 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/dongs")
public class DongSearchController {

    private final DongneService dongneService;

    @Operation(
            summary = "행정동 자동완성",
            description =
                    "q 가 행정동 이름·자치구 이름·풀 주소 중 하나라도 매칭되면 결과 포함. "
                            + "정렬: 행정동 이름 prefix > 자치구 이름 prefix > contains. "
                            + "각 항목은 `adminDongCode`, `name`, `guName`, `fullAddress` 반환. "
                            + "인증 불필요.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "자동완성 결과",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DongSuggestionListResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "statusCode": "200",
                                      "message": "행정동 자동완성 결과",
                                      "data": {
                                        "items": [
                                          {
                                            "adminDongCode": "1141051500",
                                            "name": "연희동",
                                            "guName": "서대문구",
                                            "fullAddress": "서울 서대문구 연희동"
                                          },
                                          {
                                            "adminDongCode": "1141051600",
                                            "name": "연희2동",
                                            "guName": "서대문구",
                                            "fullAddress": "서울 서대문구 연희2동"
                                          }
                                        ]
                                      }
                                    }
                                    """)
                    )
            )
    })
    @GetMapping("/search")
    public ResponseEntity<ResponseDTO<DongSuggestionListResponse>> searchDongs(
            @Parameter(description = "검색어 (행정동 이름 일부)", example = "연희")
            @RequestParam(required = false) String q,
            @Parameter(description = "최대 결과 개수", example = "10")
            @RequestParam(required = false, defaultValue = "10") int limit
    ) {
        DongSuggestionListResponse response = dongneService.searchDongs(q, limit);
        return ResponseEntity.ok(
                ResponseDTO.res(HttpStatus.OK, "행정동 자동완성 결과", response));
    }
}
