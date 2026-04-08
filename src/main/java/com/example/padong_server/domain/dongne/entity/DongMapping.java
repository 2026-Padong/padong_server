package com.example.padongbe.domain.dongne.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class DongMapping {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_dong_id")
    private AdminDong adminDong;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "legal_dong_id")
    private LegalDong legalDong;

    public DongMapping(AdminDong admin, LegalDong legal) {
        this.adminDong = admin;
        this.legalDong = legal;
    }
}
