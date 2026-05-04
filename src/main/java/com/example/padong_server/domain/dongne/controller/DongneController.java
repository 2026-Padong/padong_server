package com.example.padong_server.domain.dongne.controller;

import com.example.padong_server.domain.dongne.service.DongneService;
import com.example.padong_server.global.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/dongne")
@Tag(name = "Dongne", description = "서울시 행정동/법정동 기준 데이터 관리 API")
public class DongneController {

    private final DongneService dongneService;

    @Operation(
            summary = "동네 기준 데이터 적재",
            description = """
                    최신 dongne 적재 API입니다.
                    classpath의 서울시 행정동, 법정동, 행정동-법정동 매핑 CSV를 읽어
                    admin_dong, legal_dong, dong_mapping 데이터를 재적재합니다.
                    생활이동, 주거 실거래가, 인구, 경계 조회에서 사용하는 기준 데이터입니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "동네 기준 데이터 적재 성공",
                    content = @Content(
                            mediaType = MediaType.TEXT_PLAIN_VALUE,
                            schema = @Schema(implementation = String.class),
                            examples = @ExampleObject(value = "동네 데이터 추가 완료")
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "CSV 파싱 실패, 중복 코드, 매핑 대상 코드 누락 등 적재 실패",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "code": "INTERNAL_SERVER_ERROR",
                                              "message": "서버 내부 오류가 발생했습니다."
                                            }
                                            """
                            )
                    )
            )
    })
    @PostMapping("/data")
    public ResponseEntity<String> addDongneData() {
        dongneService.addDongneDate();
        return ResponseEntity.ok("동네 데이터 추가 완료");
    }

    /* legacy */
}
