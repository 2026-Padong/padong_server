package com.example.padong_server.domain.dongne.controller;

import com.example.padong_server.domain.dongne.dto.DongneDetailResponse;
import com.example.padong_server.domain.dongne.service.DongneDetailService;
import com.example.padong_server.domain.dongne.service.DongneService;
import com.example.padong_server.domain.dongneLike.dto.DongneLikeToggleResponse;
import com.example.padong_server.domain.dongneLike.service.DongneLikeService;
import com.example.padong_server.domain.oauth.entity.CustomUserDetails;
import com.example.padong_server.global.ResponseDTO;
import com.example.padong_server.global.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/dongne")
@Tag(name = "Dongne", description = "서울시 행정동/법정동 기준 데이터 관리 API")
public class DongneController {

    private final DongneService dongneService;
    private final DongneDetailService dongneDetailService;
    private final DongneLikeService dongneLikeService;

    @Operation(
            summary = "동네 기준 데이터 적재",
            description = """
                    최신 dongne 적재 API입니다.
                    classpath의 서울시 행정동, 법정동, 행정동-법정동 매핑 CSV를 읽어
                    admin_dong, legal_dong, dong_mapping 데이터를 적재합니다.
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

    @GetMapping("/detail")
    @Operation(summary = "동네 상세 조회")
    public ResponseEntity<ResponseDTO<DongneDetailResponse>> getDongneDetail(
            @RequestParam String adminDongCode,
            @RequestParam(required = false) String workAdminDongCode,
            @RequestParam(required = false) Long userId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long resolvedUserId = userDetails != null ? userDetails.getUserId() : userId;
        return ResponseEntity.ok(
                dongneDetailService.getDetail(adminDongCode, workAdminDongCode, resolvedUserId));
    }

    @PostMapping("/likes")
    @Operation(summary = "동네 좋아요 토글")
    public ResponseEntity<ResponseDTO<DongneLikeToggleResponse>> toggleDongneLike(
            @RequestParam String adminDongCode,
            @RequestParam(required = false) Long userId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long resolvedUserId = userDetails != null ? userDetails.getUserId() : userId;
        DongneLikeToggleResponse response =
                dongneLikeService.toggleLike(adminDongCode, resolvedUserId);
        return ResponseEntity.ok(
                ResponseDTO.res(HttpStatus.OK, "동네 좋아요 상태가 변경되었습니다.", response));
    }
}
