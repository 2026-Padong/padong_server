package com.example.padong_server.domain.menu.controller;

import com.example.padong_server.domain.menu.dto.MenuCreateRequest;
import com.example.padong_server.domain.menu.dto.MenuResponse;
import com.example.padong_server.domain.menu.dto.MenuUpdateRequest;
import com.example.padong_server.domain.menu.service.MenuService;
import com.example.padong_server.global.ResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "메뉴", description = "가게의 메뉴 CRUD")
@RestController
@RequiredArgsConstructor
@RequestMapping("/menus")
public class MenuController {

    private final MenuService menuService;

    @Operation(summary = "메뉴 등록", description = "JSON body. 이름·가격만 받음.")
    @PostMapping
    public ResponseEntity<ResponseDTO<MenuResponse>> createMenu(
            @Valid @RequestBody MenuCreateRequest request) {
        MenuResponse response = menuService.createMenu(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseDTO.res(HttpStatus.CREATED, "메뉴 등록 성공", response));
    }

    @Operation(summary = "가게별 메뉴 목록 조회")
    @GetMapping
    public ResponseEntity<ResponseDTO<List<MenuResponse>>> getMenus(@RequestParam Long storeId) {
        return ResponseEntity.ok(
                ResponseDTO.res(HttpStatus.OK, "메뉴 목록 조회 성공", menuService.getMenus(storeId)));
    }

    @Operation(summary = "메뉴 수정", description = "JSON body. 이름·가격만 수정.")
    @PutMapping("/{menuId}")
    public ResponseEntity<ResponseDTO<MenuResponse>> updateMenu(
            @PathVariable Long menuId, @Valid @RequestBody MenuUpdateRequest request) {
        return ResponseEntity.ok(
                ResponseDTO.res(HttpStatus.OK, "메뉴 수정 성공", menuService.updateMenu(menuId, request)));
    }

    @Operation(summary = "메뉴 삭제")
    @DeleteMapping("/{menuId}")
    public ResponseEntity<Void> deleteMenu(@PathVariable Long menuId) {
        menuService.deleteMenu(menuId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "메뉴 품절 토글", description = "menuId 의 sold_out 을 지정 값으로 설정.")
    @PutMapping("/sold-out")
    public ResponseEntity<ResponseDTO<MenuResponse>> toggleSoldOut(
            @RequestParam Long menuId, @RequestParam boolean soldOut) {
        return ResponseEntity.ok(
                ResponseDTO.res(
                        HttpStatus.OK,
                        soldOut ? "메뉴 품절 처리 성공" : "메뉴 품절 해제 성공",
                        menuService.toggleSoldOut(menuId, soldOut)));
    }
}
