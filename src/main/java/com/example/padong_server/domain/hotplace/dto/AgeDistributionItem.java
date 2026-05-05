package com.example.padong_server.domain.hotplace.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AgeDistributionItem {

    private final String ageGroup;
    private final String rate;
}
