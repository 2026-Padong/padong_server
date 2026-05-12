package com.example.padong_server.domain.picture.dto;

public record PictureMappingResponse(
        long totalPictureCount,
        int processedCount,
        int mappedPictureCount,
        int skippedUnresolvedCount,
        int resolvedByParenthesisCount,
        int resolvedByAddressApiCount,
        int offset,
        int limit,
        boolean hasNext
) {
}
