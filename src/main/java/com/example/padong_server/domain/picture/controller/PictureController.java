package com.example.padong_server.domain.picture.controller;

import com.example.padong_server.domain.picture.dto.AdminDongPictureResponse;
import com.example.padong_server.domain.picture.dto.PictureImportResponse;
import com.example.padong_server.domain.picture.service.PictureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Pictures", description = "행정동별 관광 사진 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/pictures")
public class PictureController {

    private final PictureService pictureService;

    @Operation(
            summary = "관광 사진 적재",
            description = "TourAPI에서 서울특별시 관광 데이터의 contentId, addr1, title, firstImage를 수집하고 행정동 컬럼까지 저장합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "관광 사진 적재 성공"),
            @ApiResponse(responseCode = "503", description = "주소 API 또는 관광 API 키 없음"),
            @ApiResponse(responseCode = "502", description = "주소 API 또는 관광 API 호출 실패")
    })
    @PostMapping("/data")
    public ResponseEntity<PictureImportResponse> importPictures() {
        return ResponseEntity.ok(pictureService.importPictures());
    }

    @Operation(
            summary = "행정동별 관광 사진 조회",
            description = "저장된 관광 사진 중 행정동 코드와 일치하는 목록을 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "관광 사진 조회 성공",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AdminDongPictureResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "adminDongCode": "1168052100",
                                              "adminDongName": "삼성1동",
                                              "pictureCount": 1,
                                              "pictures": [
                                                {
                                                  "contentId": "2456536",
                                                  "title": "강남 마이스 관광특구",
                                                  "roadAddress": "서울특별시 강남구 영동대로 513 (삼성동)",
                                                  "firstImageUrl": "http://tong.visitkorea.or.kr/cms/resource/13/3464913_image2_1.jpg",
                                                  "adminDongCode": "1168052100",
                                                  "adminDongName": "삼성1동"
                                                }
                                              ]
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "404", description = "해당 행정동 사진 없음")
    })
    @GetMapping
    public ResponseEntity<AdminDongPictureResponse> getPicturesByAdminDongCode(
            @Parameter(description = "행정동 코드", example = "1168052100")
            @RequestParam String adminDongCode
    ) {
        return ResponseEntity.ok(pictureService.getPicturesByAdminDongCode(adminDongCode));
    }
}
