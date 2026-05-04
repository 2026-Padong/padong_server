package com.example.padong_server.global.client.seoul;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SeoulRealtimeData {

    private final String areaCd;
    private final String areaNm;
    private final String thumbnail;
    private final String roadAddr;
    private final Double areaPpltnMin;
    private final Double areaPpltnMax;
    private final String areaCongestLvl;
    private final String areaCongestMsg;
    private final String fcstYn;
    private final Double fcstPpltnMin;
    private final Double fcstPpltnMax;
    private final String fcstTime;
    private final Double malePpltnRate;
    private final Double femalePpltnRate;
    private final Double ppltnRate10;
    private final Double ppltnRate20;
    private final Double ppltnRate30;
    private final Double ppltnRate40;
    private final Double ppltnRate50;
    private final Double ppltnRate60;
    private final Double ppltnRate70;
    private final String roadTrafficIdx;
    private final Double roadTrafficSpd;
    private final String weatherStatus;
    private final Double temperature;
    private final Double sensibleTemperature;
    private final Double humidity;
    private final Double pm10;
    private final String pm10Status;
    private final Double rainChance;
    private final String eventNm;
    private final List<String> subwayStationNames;
    private final List<String> subwayLines;
    private final List<String> busStopNames;
    private final List<String> bikeStationNames;
}
