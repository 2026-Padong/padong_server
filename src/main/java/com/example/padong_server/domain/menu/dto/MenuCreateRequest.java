package com.example.padong_server.domain.menu.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MenuCreateRequest(
        @Schema(description = "가게 ID", example = "1") @NotNull Long storeId,
        @Schema(description = "메뉴 이름", example = "모둠빵 세트") @NotBlank String name,
        @Schema(description = "판매 가격 (원)", example = "5000") @NotNull @Min(0) Integer price) {}
