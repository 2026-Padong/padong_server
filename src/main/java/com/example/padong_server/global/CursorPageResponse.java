package com.example.padong_server.global;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 커서 기반 페이지네이션 응답. {@code cursor} 는 다음 페이지 요청 시 그대로 쿼리에 실어 보내면 됨.
 * 정렬은 단조 감소하는 PK (예: like.id DESC) 기준 — tie-break 필요 없음.
 */
@Schema(description = "커서 기반 페이지네이션 응답")
public record CursorPageResponse<T>(
        @Schema(description = "현재 페이지 아이템 목록") List<T> items,
        @Schema(description = "다음 페이지 요청 시 사용할 cursor. 마지막 페이지면 null", example = "42")
                Long nextCursor,
        @Schema(description = "다음 페이지 존재 여부", example = "true") boolean hasNext) {

    /**
     * 서비스 레이어에서 LIMIT size+1 로 가져온 결과를 받아 자르고 nextCursor 계산.
     *
     * @param fetched LIMIT(size+1) 으로 가져온 row
     * @param size 페이지 크기
     * @param cursorFn 마지막 row 에서 cursor 추출 함수 (보통 row::getId)
     */
    public static <E, T> CursorPageResponse<T> from(
            List<E> fetched,
            int size,
            java.util.function.Function<E, Long> cursorFn,
            java.util.function.Function<E, T> mapper) {
        boolean hasNext = fetched.size() > size;
        List<E> page = hasNext ? fetched.subList(0, size) : fetched;
        Long nextCursor = hasNext ? cursorFn.apply(page.get(page.size() - 1)) : null;
        return new CursorPageResponse<>(page.stream().map(mapper).toList(), nextCursor, hasNext);
    }
}
