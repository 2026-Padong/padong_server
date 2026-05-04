package com.example.padong_server.domain.hotplace.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RoadTrafficInfo {

    private final String status;
    private final String speed;
}
