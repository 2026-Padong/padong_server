package com.example.padongbe.domain.dongne.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class DongMappingDto {
    private String city;
    private String district;
    private String adminAreaName;
    private String adminTypeCode;
    private String adminDongName;
    private String adminDongCode;
    private String legalDongName;
    private String legalDongCode;
}
