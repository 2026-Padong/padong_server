package com.example.padong_server.domain.hotplace.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SubwayTransportInfo {

    private final String stationName;
    private final List<String> lines;
    private final Integer stationCount;
}
