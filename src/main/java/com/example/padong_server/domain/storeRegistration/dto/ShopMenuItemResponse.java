package com.example.padong_server.domain.storeRegistration.dto;

import com.example.padong_server.domain.menu.entity.Menu;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "가게 상세 페이지의 메뉴 카드 항목 (발견 흐름 전용)")
public record ShopMenuItemResponse(
        @Schema(description = "메뉴 PK", example = "7") Long id,
        @Schema(description = "메뉴명", example = "통밀 식빵") String name,
        @Schema(description = "표시 가격(원)", example = "5500") long price,
        @Schema(description = "품절 여부", example = "false") boolean soldOut) {

    public static ShopMenuItemResponse from(Menu menu) {
        return new ShopMenuItemResponse(
                menu.getId(), menu.getMenuInfo(), menu.getPrice(), menu.isSoldOut());
    }
}
