package com.example.padong_server.domain.menu.dto;

import com.example.padong_server.domain.menu.entity.Menu;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "메뉴 응답")
public class MenuResponse {

    @Schema(description = "메뉴 PK", example = "7") private Long id;
    @Schema(description = "소속 가게 ID", example = "12") private Long storeId;
    @Schema(description = "메뉴 이름", example = "통밀 식빵") private String name;
    @Schema(description = "판매 가격 (원)", example = "5500") private Integer price;
    @Schema(description = "품절 여부", example = "false") private boolean soldOut;

    public static MenuResponse from(Menu menu) {
        return MenuResponse.builder()
                .id(menu.getId())
                .storeId(menu.getStore().getId())
                .name(menu.getName())
                .price(menu.getPrice())
                .soldOut(menu.isSoldOut())
                .build();
    }
}
