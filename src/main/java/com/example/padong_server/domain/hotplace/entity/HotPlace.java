package com.example.padong_server.domain.hotplace.entity;

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

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "hot_place",
        indexes = {
                @Index(name = "idx_realtime_place_area_code", columnList = "areaCode"),
                @Index(name = "idx_realtime_place_gu_name", columnList = "guName"),
                @Index(name = "idx_realtime_place_category", columnList = "category")
        }
)
public class HotPlace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String category;
    private Integer placeNumber;
    private String areaCode;
    private String areaName;
    private String englishName;
    private String guName;
}
