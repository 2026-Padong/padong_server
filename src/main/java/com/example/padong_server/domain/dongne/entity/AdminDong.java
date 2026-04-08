package com.example.padongbe.domain.dongne.entity;

import com.example.padongbe.domain.dongne.dto.DongMappingDto;
import com.example.padongbe.domain.safetyGrade.entity.SafetyGrade;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
public class AdminDong {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String city;
    private String district;
    private String adminAreaName;
    private String adminDongName;
    private String adminTypeCode;
    private String adminDongCode;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "safety_grade_id")
    private SafetyGrade safetyGrade;

    @OneToMany(mappedBy = "adminDong")
    private List<DongMapping> dongMappingList = new ArrayList<>();

    public AdminDong(DongMappingDto dto) {
        this.city = dto.getCity();
        this.district = dto.getDistrict();
        this.adminAreaName = dto.getAdminAreaName();
        this.adminDongName = dto.getAdminDongName();
        this.adminDongCode = dto.getAdminDongCode();
        this.adminTypeCode = dto.getAdminTypeCode();
    }

}
