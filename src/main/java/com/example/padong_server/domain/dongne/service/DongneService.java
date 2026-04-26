package com.example.padong_server.domain.dongne.service;

import com.example.padong_server.domain.dongne.entity.AdminDong;
import com.example.padong_server.domain.dongne.entity.DongMapping;
import com.example.padong_server.domain.dongne.entity.LegalDong;
import com.example.padong_server.domain.dongne.repository.AdminDongRepository;
import com.example.padong_server.domain.dongne.repository.DongMappingRepository;
import com.example.padong_server.domain.dongne.repository.LegalDongRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DongneService {

    private final AdminDongRepository adminDongRepository;
    private final LegalDongRepository legalDongRepository;
    private final DongMappingRepository dongMappingRepository;
    private final DongneImportService dongneImportService;

    public void addDongneDate() {
        dongneImportService.importData();
    }

    public AdminDong findAdminDongByCode(String adminDongCode) {
        return adminDongRepository.findByAdminDongCode(adminDongCode)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 행정동 코드입니다."));
    }

    public AdminDong findAdminDongByTypeCode(String adminTypeCode) {
        switch (adminTypeCode) {
            case "11160640" -> adminTypeCode = "11160751";
            case "11230740" -> adminTypeCode = "11230511";
            case "11160720" -> adminTypeCode = "11160761";
            case "11170680" -> adminTypeCode = "11170730";
            case "11250510" -> adminTypeCode = "11250750";
            case "11250520" -> adminTypeCode = "11250760";
        }
        String finalAdminTypeCode = adminTypeCode;
        return adminDongRepository.findFirstByAdminDongCodeStartingWith(adminTypeCode)
                .orElseThrow(() -> new IllegalArgumentException(finalAdminTypeCode + ": 존재하지 않는 행정분류 코드입니다."));
    }

    public LegalDong findLegalDongByCode(String legalDongCode) {
        return legalDongRepository.findByLegalDongCode(legalDongCode)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 법정동 코드입니다."));
    }

    public AdminDong findAdminDongByAddress(String address) {
        String[] addressParts = address.split(" ");
        if (addressParts.length < 3) {
            throw new IllegalArgumentException("주소는 시/도 구/군 행정동 형식이어야 합니다.");
        }

        return adminDongRepository.findByCityNameAndDistrictNameAndAdminDongName(
                        addressParts[0],
                        addressParts[1],
                        addressParts[2]
                )
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 행정동 주소입니다."));
    }

    public LegalDong findLegalDongByAdminCode(String adminDongCode) {
        AdminDong adminDong = findAdminDongByCode(adminDongCode);
        List<DongMapping> mappings = dongMappingRepository.findByAdminDong(adminDong);
        if (mappings.isEmpty()) {
            return null;
        }
        return mappings.get(0).getLegalDong();
    }

    /* legacy */
}
