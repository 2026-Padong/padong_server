package com.example.padong_server.domain.hotplace.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
                @Index(name = "idx_hot_place_area_nm", columnList = "areaNm"),
                @Index(name = "idx_realtime_place_gu_name", columnList = "guName"),
                @Index(name = "idx_realtime_place_category", columnList = "category")
        }
)
public class HotPlace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String areaNm;

    @Column(nullable = false, length = 30)
    private String guName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Category category;

    @Column(nullable = false, precision = 10)
    private Double latitude;

    @Column(nullable = false, precision = 10)
    private Double longitude;
}
