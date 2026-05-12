package com.example.padong_server.domain.storeRegistration.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@Schema(description = "갤러리 이미지 순서 변경 요청")
public record StoreImageReorderRequest(
        @Schema(description = "새 순서대로 나열한 storeImage ID 배열", example = "[13, 12, 15]")
                @NotEmpty List<Long> ids) {}
