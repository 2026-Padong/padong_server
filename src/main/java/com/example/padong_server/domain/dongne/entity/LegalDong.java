package com.example.padong_server.domain.dongne.entity;

import com.example.padong_server.domain.dongne.dto.LegalDongCsvRow;
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
                @UniqueConstraint(name = "uk_legal_dong_code", columnNames = "legal_dong_code")
        }
)
public class LegalDong {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "city_name")
    private String cityName;

    @Column(nullable = false, name = "district_name")
    private String districtName;

    @Column(nullable = false)
    private String legalDongName;

    @Column(name = "legal_dong_code", nullable = false, unique = true)
    private String legalDongCode;

    @OneToMany(mappedBy = "legalDong")
    private List<DongMapping> mappings = new ArrayList<>();

    public LegalDong(LegalDongCsvRow row) {
        this.cityName = row.cityName();
        this.districtName = row.districtName();
        this.legalDongName = row.legalDongName();
        this.legalDongCode = row.legalDongCode();
    }
}
