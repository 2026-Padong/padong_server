package com.example.padong_server.domain.storeRegistration.controller;

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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Store Registration", description = "사장님이 가게 정보를 등록하고 조회·수정·삭제하는 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stores")
public class StoreRegistrationController {

    private final StoreRegistrationService storeRegistrationService;

    @Operation(
            summary = "가게 등록",
            description = "사장님이 가게명, 주소, 전화번호, 운영시간을 쿼리파라미터로 전달해 가게 정보를 등록합니다."
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
                                                "name": "파리바게뜨 연희안산점",
                                                "address": "서울 서대문구 연희로 11길 24",
                                                "phoneNumber": "0507-2093-9485",
                                                "operatingHours": "10:00 ~ 15:00 (매일)"
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
            @Parameter(description = "가게명", example = "파리바게뜨 연희안산점")
            @RequestParam String name,
            @Parameter(description = "가게 주소", example = "서울 서대문구 연희로 11길 24")
            @RequestParam String address,
            @Parameter(description = "가게 전화번호", example = "0507-2093-9485")
            @RequestParam String phoneNumber,
            @Parameter(description = "운영시간", example = "10:00 ~ 15:00 (매일)")
            @RequestParam String operatingHours
    ) {
        StoreRegistrationCreateRequest request = new StoreRegistrationCreateRequest(
                name,
                address,
                phoneNumber,
                operatingHours
        );
        StoreRegistrationResponse response = storeRegistrationService.createStore(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseDTO.res(HttpStatus.CREATED, "가게 등록이 완료되었습니다.", response));
    }

    @Operation(
            summary = "가게 상세 조회",
            description = "사장님이 수정 화면에 진입할 때 사용할 가게 상세 정보를 storeId 쿼리파라미터로 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "가게 상세 조회 성공"),
            @ApiResponse(responseCode = "404", description = "가게 정보 없음")
    })
    @GetMapping
    public ResponseEntity<ResponseDTO<StoreRegistrationResponse>> getStore(
            @Parameter(description = "가게 ID", example = "1")
            @RequestParam Long storeId
    ) {
        StoreRegistrationResponse response = storeRegistrationService.getStore(storeId);
        return ResponseEntity.ok(ResponseDTO.res(HttpStatus.OK, "가게 상세 조회에 성공했습니다.", response));
    }

    @Operation(
            summary = "가게 기본 정보 수정",
            description = "사장님이 상세 조회 화면에서 storeId와 함께 가게명, 주소, 전화번호, 운영시간을 쿼리파라미터로 전달해 수정합니다."
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
            @Parameter(description = "가게명", example = "파리바게뜨 연희안산점")
            @RequestParam String name,
            @Parameter(description = "가게 주소", example = "서울 서대문구 연희로 11길 24")
            @RequestParam String address,
            @Parameter(description = "가게 전화번호", example = "0507-2093-9485")
            @RequestParam String phoneNumber,
            @Parameter(description = "운영시간", example = "10:00 ~ 15:00 (매일)")
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
            description = "사장님이 storeId 쿼리파라미터로 등록한 가게 정보를 삭제합니다."
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
