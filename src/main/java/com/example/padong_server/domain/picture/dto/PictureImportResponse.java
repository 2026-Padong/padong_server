package com.example.padong_server.domain.picture.dto;

public record PictureImportResponse(
        int fetchedContentCount,
        int savedPictureCount,
        int skippedNoImageCount,
        int usedTourApiCallCount,
        int remainingTourApiCallCount
) {
}
