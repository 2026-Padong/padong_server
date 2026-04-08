package com.example.padong_server.domain.dongne.entity;

import com.example.padongbe.domain.dongne.dto.DongMappingDto;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
public class LegalDong {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String legalDongName;
    private String legalDongCode;

    @OneToMany(mappedBy = "legalDong")
    private List<DongMapping> mappings = new ArrayList<>();

    public LegalDong(DongMappingDto dto) {
        this.legalDongName = dto.getLegalDongName();
        this.legalDongCode = dto.getLegalDongCode();
    }
}
