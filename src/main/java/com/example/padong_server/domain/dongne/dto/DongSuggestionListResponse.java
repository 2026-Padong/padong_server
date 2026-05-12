package com.example.padong_server.domain.dongne.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "행정동 자동완성 응답")
public record DongSuggestionListResponse(
        @Schema(description = "자동완성 결과 (최대 limit 개)") List<DongSuggestionItem> items) {}
