package com.example.padong_server.domain.activityMobility.entity;

import jakarta.persistence.Column;
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
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "safety_index",
        indexes = {
                @Index(name = "idx_safety_index_city_district", columnList = "cityName,districtName")
        }
)
public class SafetyIndex {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String cityName;

    @Column(nullable = false, unique = true, length = 30)
    private String districtName;

    @Column(nullable = false)
    private Integer trafficAccidentScore;

    @Column(nullable = false)
    private Integer fireScore;

    @Column(nullable = false)
    private Integer crimeScore;

    @Column(nullable = false)
    private Integer lifeSafetyScore;

    @Column(nullable = false)
    private Integer suicideScore;

    @Column(nullable = false)
    private Integer infectiousDiseaseScore;

    public double averageScore() {
        return (trafficAccidentScore
                + fireScore
                + crimeScore
                + lifeSafetyScore
                + suicideScore
                + infectiousDiseaseScore) / 6.0;
    }
}
