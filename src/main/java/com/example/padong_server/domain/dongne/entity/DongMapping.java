package com.example.padong_server.domain.dongne.entity;

import com.example.padong_server.domain.dongne.dto.DongMappingCsvRow;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_admin_legal_mapping",
                        columnNames = {"admin_dong_id", "legal_dong_id"}
                )
        }
)
public class DongMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_dong_id")
    private AdminDong adminDong;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "legal_dong_id")
    private LegalDong legalDong;

    public DongMapping(AdminDong adminDong, LegalDong legalDong, DongMappingCsvRow row) {
        this.adminDong = adminDong;
        this.legalDong = legalDong;
    }
}
