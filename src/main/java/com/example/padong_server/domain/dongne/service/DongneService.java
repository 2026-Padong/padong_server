package com.example.padongbe.domain.dongne.service;

import com.example.padongbe.domain.activityMobility.service.MobilityService;
import com.example.padongbe.domain.dongne.dto.DetailResponse;
import com.example.padongbe.domain.dongne.dto.DongMappingDto;
import com.example.padongbe.domain.dongne.entity.AdminDong;
import com.example.padongbe.domain.dongne.entity.DongMapping;
import com.example.padongbe.domain.dongne.entity.LegalDong;
import com.example.padongbe.domain.dongne.repository.AdminDongRepository;
import com.example.padongbe.domain.dongne.repository.DongMappingRepository;
import com.example.padongbe.domain.dongne.repository.LegalDongRepository;
import com.example.padongbe.domain.dongne.util.DongneDataUtil;
import com.example.padongbe.domain.safetyGrade.entity.SafetyGrade;
import com.example.padongbe.domain.safetyGrade.service.SafetyGradeService;
import com.example.padongbe.global.ResponseDTO;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DongneService {

    private final AdminDongRepository adminDongRepository;
    private final LegalDongRepository legalDongRepository;
    private final DongMappingRepository dongMappingRepository;
    private final SafetyGradeService safetyGradeService;
    private final DongneDataUtil dongneDataUtil;


    public void addDongneDate() {
        List<DongMappingDto> rows = dongneDataUtil.readDongneFromExcel();

        for (DongMappingDto dto : rows) {
            AdminDong admin = adminDongRepository.findByAdminDongCode(dto.getAdminDongCode())
                    .orElseGet(() -> adminDongRepository.save(new AdminDong(dto)));

            LegalDong legal = legalDongRepository.findByLegalDongCode(dto.getLegalDongCode())
                    .orElseGet(() -> legalDongRepository.save(new LegalDong(dto)));

            if (!dongMappingRepository.existsByAdminDongAndLegalDong(admin, legal))
                dongMappingRepository.save(new DongMapping(admin, legal));
        }

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
        return adminDongRepository.findByAdminTypeCode(adminTypeCode)
                .orElseThrow(() -> new IllegalArgumentException(finalAdminTypeCode +": 존재하지 않는 행정분류 코드입니다."));
    }

    public LegalDong findLegalDongByName(String dongCode) {
        System.out.println(dongCode);
        return legalDongRepository.findByLegalDongCode(dongCode)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 법정동 이름입니다."));
    }

    @Transactional
    public void addDongneSafetyGradeData() {
        List<AdminDong> adminDongList = adminDongRepository.findAll();
        Map<String, SafetyGrade> safetyGradeMap = safetyGradeService.getSafetyGradeMap();
        adminDongList.forEach(adminDong -> {
            SafetyGrade safetyGrade = safetyGradeMap.get(adminDong.getDistrict());
            adminDong.setSafetyGrade(safetyGrade);
            System.out.println(safetyGrade.getDistrictName());
        });
//        List<AdminDong> adminDongList = adminDongRepository.findAll();
//        adminDongList.forEach(adminDong -> {
//                    SafetyGrade safetyGrade = safetyGradeService.findByDistrictName(adminDong.getDistrict());
//                    adminDong.setSafetyGrade(safetyGrade);
//                    adminDongRepository.save(adminDong);
//                });
    }

    public AdminDong findAdminDongByAddress(String address) {
        String[] addressParts = address.split(" ");
        String city = addressParts[0];
        String district = addressParts[1];
        String dong = addressParts[2];

        return adminDongRepository.findByCityAndDistrictAndAdminAreaName(city, district, dong)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 행정동 주소입니다."));
    }

    public LegalDong findLegalDongByAdminCode(String adminCode){
        Optional<AdminDong> optional= adminDongRepository.findByAdminDongCode(adminCode);
        if (optional.isEmpty()) return null;
        else {
            log.info(optional.get().getAdminDongName());
            log.info(String.valueOf(dongMappingRepository.findByAdminDong(optional.get()).size()));
            DongMapping legalDong= dongMappingRepository.findByAdminDong(optional.get()).get(0);
            return legalDong.getLegalDong();
        }
    }
}