package com.example.padong_server.global;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Schema(description = "공통 페이징 응답")
public record PageResponse<T>(
        @Schema(description = "현재 페이지 데이터")
        List<T> content,

        @Schema(description = "현재 페이지 번호. 0부터 시작.", example = "0")
        int page,

        @Schema(description = "페이지 크기", example = "10")
        int size,

        @Schema(description = "전체 데이터 개수", example = "424")
        long totalElements,

        @Schema(description = "전체 페이지 개수", example = "43")
        int totalPages,

        @Schema(description = "첫 페이지 여부", example = "true")
        boolean first,

        @Schema(description = "마지막 페이지 여부", example = "false")
        boolean last,

        @Schema(description = "다음 페이지 존재 여부", example = "true")
        boolean hasNext,

        @Schema(description = "이전 페이지 존재 여부", example = "false")
        boolean hasPrevious
) {

    public PageResponse {
        content = content == null ? List.of() : List.copyOf(content);
    }

    public static <T> PageResponse<T> from(Page<?> page, List<T> content) {
        return of(content, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    public static <T> PageResponse<T> of(List<T> content, Pageable pageable, long totalElements) {
        return of(content, pageable.getPageNumber(), pageable.getPageSize(), totalElements);
    }

    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = calculateTotalPages(totalElements, size);
        return new PageResponse<>(
                content,
                page,
                size,
                totalElements,
                totalPages,
                page == 0,
                totalPages == 0 || page >= totalPages - 1,
                page + 1 < totalPages,
                page > 0
        );
    }

    private static int calculateTotalPages(long totalElements, int size) {
        if (totalElements <= 0 || size <= 0) {
            return 0;
        }
        return Math.toIntExact((totalElements + size - 1) / size);
    }
}
