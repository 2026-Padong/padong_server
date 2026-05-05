package com.example.padong_server.domain.menu.controller;

import com.example.padong_server.domain.menu.dto.MenuCreateRequest;
import com.example.padong_server.domain.menu.dto.MenuResponse;
import com.example.padong_server.domain.menu.dto.MenuUpdateRequest;
import com.example.padong_server.domain.menu.service.MenuService;
import com.example.padong_server.global.ResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "메뉴", description = "사장이 메뉴를 등록, 조회, 수정, 삭제하는 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/menus")
public class MenuController {

    private final MenuService menuService;

    @Operation(
            summary = "메뉴 등록",
            description = "가게 ID와 메뉴 정보를 쿼리 파라미터로 받아 메뉴를 등록합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "메뉴 등록 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDTO.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "statusCode": "201",
                                              "message": "메뉴 등록이 완료되었습니다.",
                                              "data": {
                                                "id": 1,
                                                "storeId": 1,
                                                "menuInfo": "모둠빵 세트",
                                                "originalPrice": 5000,
                                                "discountPrice": 3000,
                                                "pickupAvailableTime": "10:00 ~ 15:00",
                                                "maxParticipants": 5,
                                                "recruitmentDeadline": "픽업 30분 전",
                                                "paymentMethod": "카드 / 간편결제",
                                                "currentParticipants": 1
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "404", description = "가게를 찾을 수 없음")
    })
    @PostMapping
    public ResponseEntity<ResponseDTO<MenuResponse>> createMenu(
            @Parameter(description = "가게 ID", example = "1")
            @RequestParam Long storeId,
            @Parameter(description = "메뉴 설명", example = "모둠빵 세트")
            @RequestParam String menuInfo,
            @Parameter(description = "정가", example = "5000")
            @RequestParam Integer originalPrice,
            @Parameter(description = "할인가", example = "3000")
            @RequestParam Integer discountPrice,
            @Parameter(description = "픽업 가능 시간", example = "10:00 ~ 15:00")
            @RequestParam String pickupAvailableTime,
            @Parameter(description = "최대 모집 인원", example = "5")
            @RequestParam Integer maxParticipants,
            @Parameter(description = "모집 마감 시간", example = "픽업 30분 전")
            @RequestParam String recruitmentDeadline,
            @Parameter(description = "결제 수단", example = "카드 / 간편결제")
            @RequestParam String paymentMethod,
            @Parameter(description = "현재 참여 인원", example = "1")
            @RequestParam Integer currentParticipants
    ) {
        MenuResponse response = menuService.createMenu(
                new MenuCreateRequest(
                        storeId,
                        menuInfo,
                        originalPrice,
                        discountPrice,
                        pickupAvailableTime,
                        maxParticipants,
                        recruitmentDeadline,
                        paymentMethod,
                        currentParticipants
                )
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseDTO.res(HttpStatus.CREATED, "메뉴 등록이 완료되었습니다.", response));
    }

    @Operation(
            summary = "가게별 메뉴 목록 조회",
            description = "해당 가게에 등록된 전체 메뉴 목록을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "메뉴 목록 조회 성공"),
            @ApiResponse(responseCode = "404", description = "가게를 찾을 수 없음")
    })
    @GetMapping
    public ResponseEntity<ResponseDTO<List<MenuResponse>>> getMenus(
            @Parameter(description = "가게 ID", example = "1")
            @RequestParam Long storeId
    ) {
        List<MenuResponse> response = menuService.getMenus(storeId);
        return ResponseEntity.ok(ResponseDTO.res(HttpStatus.OK, "메뉴 목록 조회에 성공했습니다.", response));
    }

    @Operation(
            summary = "메뉴 수정",
            description = "메뉴 ID와 수정할 메뉴 정보를 받아 메뉴를 수정합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "메뉴 수정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "404", description = "메뉴를 찾을 수 없음")
    })
    @PutMapping
    public ResponseEntity<ResponseDTO<MenuResponse>> updateMenu(
            @Parameter(description = "메뉴 ID", example = "1")
            @RequestParam Long menuId,
            @Parameter(description = "메뉴 설명", example = "모둠빵 세트")
            @RequestParam String menuInfo,
            @Parameter(description = "정가", example = "4500")
            @RequestParam Integer originalPrice,
            @Parameter(description = "할인가", example = "2500")
            @RequestParam Integer discountPrice,
            @Parameter(description = "픽업 가능 시간", example = "11:00 ~ 16:00")
            @RequestParam String pickupAvailableTime,
            @Parameter(description = "최대 모집 인원", example = "4")
            @RequestParam Integer maxParticipants,
            @Parameter(description = "모집 마감 시간", example = "픽업 1시간 전")
            @RequestParam String recruitmentDeadline,
            @Parameter(description = "결제 수단", example = "카드")
            @RequestParam String paymentMethod,
            @Parameter(description = "현재 참여 인원", example = "2")
            @RequestParam Integer currentParticipants
    ) {
        MenuResponse response = menuService.updateMenu(
                menuId,
                new MenuUpdateRequest(
                        menuInfo,
                        originalPrice,
                        discountPrice,
                        pickupAvailableTime,
                        maxParticipants,
                        recruitmentDeadline,
                        paymentMethod,
                        currentParticipants
                )
        );
        return ResponseEntity.ok(ResponseDTO.res(HttpStatus.OK, "메뉴 수정이 완료되었습니다.", response));
    }

    @Operation(
            summary = "메뉴 삭제",
            description = "메뉴 ID를 받아 해당 메뉴를 삭제합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "메뉴 삭제 성공"),
            @ApiResponse(responseCode = "404", description = "메뉴를 찾을 수 없음")
    })
    @DeleteMapping
    public ResponseEntity<ResponseDTO<Void>> deleteMenu(
            @Parameter(description = "메뉴 ID", example = "1")
            @RequestParam Long menuId
    ) {
        menuService.deleteMenu(menuId);
        return ResponseEntity.ok(ResponseDTO.res(HttpStatus.OK, "메뉴 삭제가 완료되었습니다."));
    }
}
