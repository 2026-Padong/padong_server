package com.example.padong_server.domain.dongne.entity;

import java.util.ArrayList;
import java.util.List;

import com.example.padong_server.domain.dongne.dto.AdminDongCsvRow;

import com.example.padong_server.domain.subway.entity.Subway;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class AdminDong {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String cityName;

    @Column(nullable = false)
    private String districtName;

    @Column(nullable = false)
    private String adminDongName;

    @Column(nullable = false, unique = true)
    private String adminDongCode;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id")
    private Subway subway;

    @OneToMany(mappedBy = "adminDong")
    private List<DongMapping> mappings = new ArrayList<>();

    public AdminDong(AdminDongCsvRow row) {
        this(row, null);
    }

    public AdminDong(AdminDongCsvRow row, Subway subway) {
        this.cityName = row.cityName();
        this.districtName = row.districtName();
        this.adminDongName = row.adminDongName();
        this.adminDongCode = row.adminDongCode();
        this.latitude = row.latitude();
        this.longitude = row.longitude();
        this.subway = subway;
    }
}
