package com.example.padong_server.domain.rentPrice.entity;

import com.example.padongbe.domain.dongne.entity.LegalDong;
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
public class RentPrice {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
//    private String dongName;
//    private String districtName;
//    private String districtCode;
//    private String dongCode;
    @ManyToOne(fetch = FetchType.LAZY)
    private LegalDong legalDong;
    private String buildingType;
    private Long avgJeonseDeposit;
    private Long avgMonthlyDeposit;
    private Long avgMonthlyRent;

}
