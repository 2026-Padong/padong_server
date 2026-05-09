package com.example.padong_server.domain.storeRegistration.controller;

import com.example.padong_server.domain.oauth.entity.CustomUserDetails;
import com.example.padong_server.domain.storeLike.dto.StoreLikeToggleResponse;
import com.example.padong_server.domain.storeLike.service.StoreLikeService;
import com.example.padong_server.domain.storeRegistration.dto.StoreRegistrationCreateRequest;
import com.example.padong_server.domain.storeRegistration.dto.StoreRegistrationResponse;
import com.example.padong_server.domain.storeRegistration.dto.StoreRegistrationUpdateRequest;
import com.example.padong_server.domain.storeRegistration.service.StoreRegistrationService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "가게 등록", description = "사장이 가게 정보를 등록, 조회, 수정, 삭제하는 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/stores")
public class StoreRegistrationController {

    private final StoreRegistrationService storeRegistrationService;
    private final StoreLikeService storeLikeService;

    @Operation(
            summary = "가게 등록",
            description = "가게명, 주소, 전화번호, 운영시간을 쿼리 파라미터로 받아 가게를 등록합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "가게 등록 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDTO.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "statusCode": "201",
                                              "message": "가게 등록이 완료되었습니다.",
                                              "data": {
                                                "id": 1,
                                                "name": "파동 식당",
                                                "address": "서울 송파구 올림픽로 300",
                                                "phoneNumber": "0507-2093-9485",
                                                "operatingHours": "10:00 ~ 15:00",
                                                "likeCount": 0,
                                                "likedByCurrentUser": false
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "필수 입력값 누락")
    })
    @PostMapping
    public ResponseEntity<ResponseDTO<StoreRegistrationResponse>> createStore(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "가게명", example = "파동 식당")
            @RequestParam String name,
            @Parameter(description = "가게 주소", example = "서울 송파구 올림픽로 300")
            @RequestParam String address,
            @Parameter(description = "가게 전화번호", example = "0507-2093-9485")
            @RequestParam String phoneNumber,
            @Parameter(description = "운영시간", example = "10:00 ~ 15:00")
            @RequestParam String operatingHours
    ) {
        StoreRegistrationCreateRequest request = new StoreRegistrationCreateRequest(
                name,
                address,
                phoneNumber,
                operatingHours
        );
        StoreRegistrationResponse response = storeRegistrationService.createStore(userDetails.getUser(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseDTO.res(HttpStatus.CREATED, "가게 등록이 완료되었습니다.", response));
    }

    @Operation(
            summary = "가게 상세 조회",
            description = "storeId로 가게를 조회하고, userId가 있으면 해당 사용자의 좋아요 여부와 총 좋아요 수를 함께 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "가게 상세 조회 성공"),
            @ApiResponse(responseCode = "404", description = "가게 정보 없음")
    })
    @GetMapping
    public ResponseEntity<ResponseDTO<StoreRegistrationResponse>> getStore(
            @Parameter(description = "가게 ID", example = "1")
            @RequestParam Long storeId,
            @Parameter(description = "로그인한 사용자 ID", example = "101")
            @RequestParam(required = false) Long userId
    ) {
        StoreRegistrationResponse response = storeRegistrationService.getStore(storeId, userId);
        return ResponseEntity.ok(ResponseDTO.res(HttpStatus.OK, "가게 상세 조회가 성공했습니다.", response));
    }

    @Operation(
            summary = "가게 좋아요 토글",
            description = "같은 사용자가 같은 가게에 다시 요청하면 좋아요가 취소됩니다. 응답으로 현재 사용자의 좋아요 여부와 전체 좋아요 수를 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "가게 좋아요 토글 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청값"),
            @ApiResponse(responseCode = "404", description = "가게 정보 없음")
    })
    @PostMapping("/likes")
    public ResponseEntity<ResponseDTO<StoreLikeToggleResponse>> toggleStoreLike(
            @Parameter(description = "가게 ID", example = "1")
            @RequestParam Long storeId,
            @Parameter(description = "로그인한 사용자 ID", example = "101")
            @RequestParam Long userId
    ) {
        StoreLikeToggleResponse response = storeLikeService.toggleLike(storeId, userId);
        return ResponseEntity.ok(ResponseDTO.res(HttpStatus.OK, "가게 좋아요 상태가 변경되었습니다.", response));
    }

    @Operation(
            summary = "가게 기본 정보 수정",
            description = "storeId와 수정할 가게 기본 정보를 쿼리 파라미터로 받아 수정합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "가게 수정 성공"),
            @ApiResponse(responseCode = "400", description = "필수 입력값 누락"),
            @ApiResponse(responseCode = "404", description = "가게 정보 없음")
    })
    @PutMapping
    public ResponseEntity<ResponseDTO<StoreRegistrationResponse>> updateStore(
            @Parameter(description = "가게 ID", example = "1")
            @RequestParam Long storeId,
            @Parameter(description = "가게명", example = "파동 식당")
            @RequestParam String name,
            @Parameter(description = "가게 주소", example = "서울 송파구 올림픽로 300")
            @RequestParam String address,
            @Parameter(description = "가게 전화번호", example = "0507-2093-9485")
            @RequestParam String phoneNumber,
            @Parameter(description = "운영시간", example = "10:00 ~ 15:00")
            @RequestParam String operatingHours
    ) {
        StoreRegistrationUpdateRequest request = new StoreRegistrationUpdateRequest(
                name,
                address,
                phoneNumber,
                operatingHours
        );
        StoreRegistrationResponse response = storeRegistrationService.updateStore(storeId, request);
        return ResponseEntity.ok(ResponseDTO.res(HttpStatus.OK, "가게 수정이 완료되었습니다.", response));
    }

    @Operation(
            summary = "가게 삭제",
            description = "storeId를 받아 등록된 가게를 삭제합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "가게 삭제 성공"),
            @ApiResponse(responseCode = "404", description = "가게 정보 없음")
    })
    @DeleteMapping
    public ResponseEntity<ResponseDTO<Void>> deleteStore(
            @Parameter(description = "가게 ID", example = "1")
            @RequestParam Long storeId
    ) {
        storeRegistrationService.deleteStore(storeId);
        return ResponseEntity.ok(ResponseDTO.res(HttpStatus.OK, "가게 삭제가 완료되었습니다."));
    }
}
