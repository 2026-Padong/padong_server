package com.example.padong_server.domain.subway.entity;

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
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "subway_transfer_info",
        indexes = {
                @Index(name = "idx_subway_transfer_station_name", columnList = "stationName")
        }
)
public class SubwayTransferInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String line;

    @Column(nullable = false, length = 100)
    private String stationName;

    @Column(nullable = false, length = 30)
    private String transferLine;

    @Column(nullable = false)
    private Integer transferDistance;

    @Column(nullable = false, length = 10)
    private String transferTime;
}
