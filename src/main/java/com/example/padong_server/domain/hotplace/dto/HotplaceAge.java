package com.example.padong_server.domain.hotplace.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class HotplaceAge {

    private final String dominantGroup;
    private final String dominantRate;
    private final List<AgeDistributionItem> top3;
    private final AgeDetail detail;
}
