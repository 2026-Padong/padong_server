package com.example.padong_server.domain.picture.dto;

public record PictureImportResponse(
        int fetchedContentCount,
        int savedPictureCount,
        int skippedNoImageCount,
        int skippedUnresolvedCount,
        int resolvedByParenthesisCount,
        int resolvedByAddressApiCount,
        int usedTourApiCallCount,
        int remainingTourApiCallCount
) {
}
