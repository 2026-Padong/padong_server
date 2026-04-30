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

@Tag(name = "Menu", description = "APIs for store owners to create, list, update, and delete menus")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/menus")
public class MenuController {

    private final MenuService menuService;

    @Operation(
            summary = "Create menu",
            description = "Creates a menu for the given store using query parameters."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Menu created",
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
                                                "menuInfo": "Assorted bread set",
                                                "originalPrice": 5000,
                                                "discountPrice": 3000,
                                                "pickupAvailableTime": "10:00 ~ 15:00",
                                                "maxParticipants": 5,
                                                "recruitmentDeadline": "30 minutes before pickup",
                                                "paymentMethod": "Card / Easy payment",
                                                "currentParticipants": 1
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Store not found")
    })
    @PostMapping
    public ResponseEntity<ResponseDTO<MenuResponse>> createMenu(
            @Parameter(description = "Store ID", example = "1")
            @RequestParam Long storeId,
            @Parameter(description = "Menu description", example = "Assorted bread set")
            @RequestParam String menuInfo,
            @Parameter(description = "Original price", example = "5000")
            @RequestParam Integer originalPrice,
            @Parameter(description = "Discount price", example = "3000")
            @RequestParam Integer discountPrice,
            @Parameter(description = "Pickup available time", example = "10:00 ~ 15:00")
            @RequestParam String pickupAvailableTime,
            @Parameter(description = "Maximum participants", example = "5")
            @RequestParam Integer maxParticipants,
            @Parameter(description = "Recruitment deadline", example = "30 minutes before pickup")
            @RequestParam String recruitmentDeadline,
            @Parameter(description = "Payment method", example = "Card / Easy payment")
            @RequestParam String paymentMethod,
            @Parameter(description = "Current participants", example = "1")
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
            summary = "List menus by store",
            description = "Returns all menus registered for the given store."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Menu list retrieved"),
            @ApiResponse(responseCode = "404", description = "Store not found")
    })
    @GetMapping
    public ResponseEntity<ResponseDTO<List<MenuResponse>>> getMenus(
            @Parameter(description = "Store ID", example = "1")
            @RequestParam Long storeId
    ) {
        List<MenuResponse> response = menuService.getMenus(storeId);
        return ResponseEntity.ok(ResponseDTO.res(HttpStatus.OK, "메뉴 목록 조회에 성공했습니다.", response));
    }

    @Operation(
            summary = "Update menu",
            description = "Updates a menu using menuId and query parameters."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Menu updated"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Menu not found")
    })
    @PutMapping
    public ResponseEntity<ResponseDTO<MenuResponse>> updateMenu(
            @Parameter(description = "Menu ID", example = "1")
            @RequestParam Long menuId,
            @Parameter(description = "Menu description", example = "Assorted bread set")
            @RequestParam String menuInfo,
            @Parameter(description = "Original price", example = "4500")
            @RequestParam Integer originalPrice,
            @Parameter(description = "Discount price", example = "2500")
            @RequestParam Integer discountPrice,
            @Parameter(description = "Pickup available time", example = "11:00 ~ 16:00")
            @RequestParam String pickupAvailableTime,
            @Parameter(description = "Maximum participants", example = "4")
            @RequestParam Integer maxParticipants,
            @Parameter(description = "Recruitment deadline", example = "1 hour before pickup")
            @RequestParam String recruitmentDeadline,
            @Parameter(description = "Payment method", example = "Card")
            @RequestParam String paymentMethod,
            @Parameter(description = "Current participants", example = "2")
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
            summary = "Delete menu",
            description = "Deletes a menu using menuId."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Menu deleted"),
            @ApiResponse(responseCode = "404", description = "Menu not found")
    })
    @DeleteMapping
    public ResponseEntity<ResponseDTO<Void>> deleteMenu(
            @Parameter(description = "Menu ID", example = "1")
            @RequestParam Long menuId
    ) {
        menuService.deleteMenu(menuId);
        return ResponseEntity.ok(ResponseDTO.res(HttpStatus.OK, "메뉴 삭제가 완료되었습니다."));
    }
}
