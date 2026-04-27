package com.example.padong_server.global.client.seoul;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SeoulRealtimeData {

    private final String areaNm;
    private final String areaCongestLvl;
    private final String areaCongestMsg;
    private final String weatherStatus;
    private final Double temperature;
    private final Double pm10;
    private final String pm10Status;
    private final Double rainChance;
}
