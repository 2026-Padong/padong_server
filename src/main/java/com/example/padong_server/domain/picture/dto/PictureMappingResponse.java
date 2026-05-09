package com.example.padong_server.domain.picture.dto;

public record PictureMappingResponse(
        int totalPictureCount,
        int mappedPictureCount,
        int skippedUnresolvedCount,
        int resolvedByParenthesisCount,
        int resolvedByAddressApiCount
) {
}
