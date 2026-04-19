package com.example.padong_server.domain.dongne.entity;

import com.example.padong_server.domain.dongne.dto.AdminDongCsvRow;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@Table(
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_admin_dong_code", columnNames = "admin_dong_code")
        }
)
public class AdminDong {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "city_name")
    private String cityName;

    @Column(nullable = false, name = "district_name")
    private String districtName;

    @Column(nullable = false)
    private String adminDongName;

    @Column(name = "admin_dong_code", nullable = false, unique = true)
    private String adminDongCode;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @OneToMany(mappedBy = "adminDong")
    private List<DongMapping> mappings = new ArrayList<>();

    public AdminDong(AdminDongCsvRow row) {
        this.cityName = row.cityName();
        this.districtName = row.districtName();
        this.adminDongName = row.adminDongName();
        this.adminDongCode = row.adminDongCode();
        this.latitude = row.latitude();
        this.longitude = row.longitude();
    }
}
