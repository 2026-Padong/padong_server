package com.example.padong_server.domain.rentPrice.service;

import com.example.padongbe.domain.dongne.entity.AdminDong;
import com.example.padongbe.domain.dongne.entity.DongMapping;
import com.example.padongbe.domain.dongne.service.DongneService;
import com.example.padongbe.domain.rentPrice.dto.response.RentPriceDto;
import com.example.padongbe.domain.rentPrice.entity.RentPrice;
import com.example.padongbe.domain.rentPrice.repository.RentPriceRepository;
import com.example.padongbe.domain.rentPrice.util.RentPriceDataUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RentPriceService {

    private final RentPriceRepository rentPriceRepository;
    private final DongneService dongneService;
    private final RentPriceDataUtil rentPriceDataUtil;

    public void uploadRentPriceData() {
        List<RentPrice> rentPriceList = rentPriceDataUtil.readRentPricesFromExcel();
        rentPriceRepository.saveAll(rentPriceList);
    }

    public RentPriceDto getRentPriceByAdminDongCode(String adminDongCode, String buildingType) {
        AdminDong adminDong = dongneService.findAdminDongByCode(adminDongCode);
        List<DongMapping> dongMappingList = adminDong.getDongMappingList();

        long sumMonthlyRent = 0L, sumJeonseDeposit = 0L, sumMonthlyDeposit = 0L;
        int rentCount = 0, jeonseCount = 0, depositCount = 0;

        for (DongMapping dongMapping : dongMappingList) {
            Optional<RentPrice> rentPriceOpt = rentPriceRepository.findByLegalDongAndBuildingType(
                    dongMapping.getLegalDong(), convertBuildingType(buildingType));

            if (rentPriceOpt.isPresent()) {
                RentPrice rentPrice = rentPriceOpt.get();

                if (rentPrice.getAvgMonthlyRent() != null) {
                    sumMonthlyRent += rentPrice.getAvgMonthlyRent();
                    rentCount++;
                }
                if (rentPrice.getAvgJeonseDeposit() != null) {
                    sumJeonseDeposit += rentPrice.getAvgJeonseDeposit();
                    jeonseCount++;
                }
                if (rentPrice.getAvgMonthlyDeposit() != null) {
                    sumMonthlyDeposit += rentPrice.getAvgMonthlyDeposit();
                    depositCount++;
                }
            }
        }

        long avgMonthlyRent = rentCount > 0 ? sumMonthlyRent / rentCount : 0L;
        long avgJeonseDeposit = jeonseCount > 0 ? sumJeonseDeposit / jeonseCount : 0L;
        long avgMonthlyDeposit = depositCount > 0 ? sumMonthlyDeposit / depositCount : 0L;

        return RentPriceDto.builder()
                .buildingType(buildingType)
                .avgMonthlyRent(avgMonthlyRent)
                .avgJeonseDeposit(avgJeonseDeposit)
                .avgMonthlyDeposit(avgMonthlyDeposit)
                .build();
    }

    public RentPriceDto getRentPriceByAdminDongCode(String adminDongCode) {
        AdminDong adminDong = dongneService.findAdminDongByCode(adminDongCode);
        List<DongMapping> dongMappingList = adminDong.getDongMappingList();

        long sumMonthlyRent = 0L, sumJeonseDeposit = 0L, sumMonthlyDeposit = 0L;
        int rentCount = 0, jeonseCount = 0, depositCount = 0;

        String[] buildingTypes = {"apartment", "officetel", "villa"};

        for (DongMapping dongMapping : dongMappingList) {
            for (String type : buildingTypes) {
                Optional<RentPrice> rentPriceOpt = rentPriceRepository.findByLegalDongAndBuildingType(
                        dongMapping.getLegalDong(), convertBuildingType(type));

                if (rentPriceOpt.isPresent()) {
                    RentPrice rentPrice = rentPriceOpt.get();

                    if (rentPrice.getAvgMonthlyRent() != null) {
                        sumMonthlyRent += rentPrice.getAvgMonthlyRent();
                        rentCount++;
                    }
                    if (rentPrice.getAvgJeonseDeposit() != null) {
                        sumJeonseDeposit += rentPrice.getAvgJeonseDeposit();
                        jeonseCount++;
                    }
                    if (rentPrice.getAvgMonthlyDeposit() != null) {
                        sumMonthlyDeposit += rentPrice.getAvgMonthlyDeposit();
                        depositCount++;
                    }
                }
            }
        }

        long avgMonthlyRent = rentCount > 0 ? sumMonthlyRent / rentCount : 0L;
        long avgJeonseDeposit = jeonseCount > 0 ? sumJeonseDeposit / jeonseCount : 0L;
        long avgMonthlyDeposit = depositCount > 0 ? sumMonthlyDeposit / depositCount : 0L;

        return RentPriceDto.builder()
                .buildingType("all")
                .avgMonthlyRent(avgMonthlyRent)
                .avgJeonseDeposit(avgJeonseDeposit)
                .avgMonthlyDeposit(avgMonthlyDeposit)
                .build();
    }

    private String convertBuildingType(String buildingType) {
        return switch (buildingType) {
            case "apartment" -> "아파트";
            case "officetel" -> "오피스텔";
            case "villa" -> "주택";
            default -> throw new IllegalStateException("Unexpected value: " + buildingType);
        };
    }

}
