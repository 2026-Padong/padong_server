package com.example.padong_server.domain.realtimedata.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "realtime_data",
        indexes = {
                @Index(name = "idx_realtime_weather_area_code", columnList = "areaCode"),
                @Index(name = "idx_realtime_weather_measured_at", columnList = "measuredAt")
        }
)
public class RealtimeData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String areaCode;
    private String areaName;
    private String hotspotName;
    private String areaCongestLevel;
    private String areaCongestMessage;
    private String weatherStatus;
    private Double temperature;
    private Double pm10;
    private Double rainChance;
    private LocalDateTime measuredAt;
}
