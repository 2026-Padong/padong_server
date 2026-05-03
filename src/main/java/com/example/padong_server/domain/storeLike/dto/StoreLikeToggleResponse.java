package com.example.padong_server.domain.storeLike.dto;

import lombok.Builder;

@Builder
public record StoreLikeToggleResponse(
        Long storeId,
        Long userId,
        boolean liked,
        long likeCount
) {
}
