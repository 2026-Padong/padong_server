package com.example.padong_server.domain.subway.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subway {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 호선 (예: 2호선)
    @Column(nullable = false)
    private String line;

    // 역번호 (예: 201)
    @Column(nullable = false)
    private String stationCode;

    // 역 이름
    @Column(nullable = false)
    private String stationName;

    // 출근 시간 혼잡도 합
    @Column(nullable = false)
    private Double morningCongestion;

    // 퇴근 시간 혼잡도 합
    @Column(nullable = false)
    private Double eveningCongestion;

    // 위도
    @Column(nullable = false)
    private Double latitude;

    // 경도
    @Column(nullable = false)
    private Double longitude;


}


