package com.example.padong_server.domain.dongneLike.dto;

import lombok.Builder;

@Builder
public record DongneLikeToggleResponse(
        Long adminDongId,
        String adminDongCode,
        Long userId,
        boolean liked,
        long likeCount
) {
}
