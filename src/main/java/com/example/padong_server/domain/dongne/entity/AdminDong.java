package com.example.padong_server.domain.dongne.entity;

import java.util.ArrayList;
import java.util.List;

import com.example.padong_server.domain.dongne.dto.AdminDongCsvRow;
import com.example.padong_server.domain.news.entity.NewsArticle;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
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

    @OneToMany(mappedBy = "adminDong")
    private List<DongMapping> mappings = new ArrayList<>();

    @OneToMany(mappedBy = "adminDong")
    private List<NewsArticle> newsArticles = new ArrayList<>();

    public AdminDong(AdminDongCsvRow row) {
        this.cityName = row.cityName();
        this.districtName = row.districtName();
        this.adminDongName = row.adminDongName();
        this.adminDongCode = row.adminDongCode();
        this.latitude = row.latitude();
        this.longitude = row.longitude();
    }
}
