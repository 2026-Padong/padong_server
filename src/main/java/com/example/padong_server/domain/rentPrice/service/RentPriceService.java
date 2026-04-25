package com.example.padong_server.domain.rentPrice.service;

import com.example.padong_server.domain.dongne.entity.AdminDong;
import com.example.padong_server.domain.dongne.repository.AdminDongRepository;
import com.example.padong_server.domain.rentPrice.dto.response.AdminResidencePriceDetailResponse;
import com.example.padong_server.domain.rentPrice.dto.response.AdminResidencePriceSummaryItemResponse;
import com.example.padong_server.domain.rentPrice.dto.response.BuildingTypeResidencePriceResponse;
import com.example.padong_server.domain.rentPrice.dto.response.MonthlyRentDisplayValueResponse;
import com.example.padong_server.domain.rentPrice.dto.response.ResidenceDisplayValueResponse;
import com.example.padong_server.domain.rentPrice.dto.response.ResidenceTypeResponse;
import com.example.padong_server.domain.rentPrice.entity.AdminRentPrice;
import com.example.padong_server.domain.rentPrice.entity.ResidenceBuildingType;
import com.example.padong_server.domain.rentPrice.entity.RentPrice;
import com.example.padong_server.domain.rentPrice.policy.RentPriceDisplayPolicy;
import com.example.padong_server.domain.rentPrice.repository.AdminRentPriceRepository;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RentPriceService {

    private static final String PERIOD_LABEL = "최근 2년 기준";
    private static final String CONTRACT_PERIOD_START = "2024-04-18";
    private static final String CONTRACT_PERIOD_END = "2026-04-17";

    private final AdminDongRepository adminDongRepository;
    private final AdminRentPriceRepository adminRentPriceRepository;
    private final RentPriceDisplayPolicy rentPriceDisplayPolicy;

    public List<AdminResidencePriceSummaryItemResponse> getSummaries(List<String> requestedAdminDongCodes) {
        if (requestedAdminDongCodes == null) {
            throw new IllegalArgumentException("행정동 코드는 비어 있을 수 없습니다.");
        }
        List<String> adminDongCodes = sanitizeAdminDongCodes(requestedAdminDongCodes);
        if (adminDongCodes.isEmpty()) {
            return List.of();
        }

        Map<String, AdminDong> adminDongByCode = loadAdminDongByCode(adminDongCodes);
        Map<String, Map<ResidenceBuildingType, AdminRentPrice>> statsByAdminDongCode =
                groupByAdminDongCode(adminRentPriceRepository.findAllByAdminDongAdminDongCodeIn(adminDongCodes));

        List<AdminResidencePriceSummaryItemResponse> responses = new ArrayList<>();
        for (String adminDongCode : adminDongCodes) {
            AdminDong adminDong = adminDongByCode.get(adminDongCode);
            responses.add(buildSummary(adminDong, statsByAdminDongCode.getOrDefault(adminDongCode, Map.of())));
        }
        return responses;
    }

    public AdminResidencePriceDetailResponse getDetail(String adminDongCode) {
        String sanitizedCode = sanitizeAdminDongCode(adminDongCode);
        AdminDong adminDong = adminDongRepository.findByAdminDongCode(sanitizedCode)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 행정동 코드입니다: " + sanitizedCode));

        Map<ResidenceBuildingType, AdminRentPrice> statsByBuildingType =
                mapByBuildingType(adminRentPriceRepository.findAllByAdminDongAdminDongCode(sanitizedCode));

        List<BuildingTypeResidencePriceResponse> buildingTypes = new ArrayList<>();
        for (ResidenceBuildingType buildingType : ResidenceBuildingType.values()) {
            BuildingTypeResidencePriceResponse response = buildBuildingTypeResponse(
                    buildingType,
                    statsByBuildingType.get(buildingType)
            );
            buildingTypes.add(response);
        }

        ResidenceBuildingType dominantResidenceType = determineDominantResidenceType(statsByBuildingType);
        return new AdminResidencePriceDetailResponse(
                adminDong.getAdminDongCode(),
                PERIOD_LABEL,
                CONTRACT_PERIOD_START,
                CONTRACT_PERIOD_END,
                true,
                toTypeResponse(dominantResidenceType),
                List.copyOf(buildingTypes)
        );
    }

    private List<String> sanitizeAdminDongCodes(List<String> requestedAdminDongCodes) {
        List<String> adminDongCodes = new ArrayList<>();
        for (String adminDongCode : requestedAdminDongCodes) {
            adminDongCodes.add(sanitizeAdminDongCode(adminDongCode));
        }
        return adminDongCodes;
    }

    private String sanitizeAdminDongCode(String adminDongCode) {
        if (adminDongCode == null || adminDongCode.trim().isEmpty()) {
            throw new IllegalArgumentException("행정동 코드는 비어 있을 수 없습니다.");
        }
        return adminDongCode.trim();
    }

    private Map<String, AdminDong> loadAdminDongByCode(Collection<String> adminDongCodes) {
        Map<String, AdminDong> adminDongByCode = new LinkedHashMap<>();
        for (AdminDong adminDong : adminDongRepository.findAllByAdminDongCodeIn(adminDongCodes)) {
            adminDongByCode.put(adminDong.getAdminDongCode(), adminDong);
        }
        for (String adminDongCode : adminDongCodes) {
            if (!adminDongByCode.containsKey(adminDongCode)) {
                throw new IllegalArgumentException("존재하지 않는 행정동 코드입니다: " + adminDongCode);
            }
        }
        return adminDongByCode;
    }

    private Map<String, Map<ResidenceBuildingType, AdminRentPrice>> groupByAdminDongCode(
            List<AdminRentPrice> stats
    ) {
        Map<String, Map<ResidenceBuildingType, AdminRentPrice>> grouped = new HashMap<>();
        for (AdminRentPrice stat : stats) {
            grouped.computeIfAbsent(stat.getAdminDong().getAdminDongCode(), ignored -> new EnumMap<>(ResidenceBuildingType.class))
                    .put(ResidenceBuildingType.fromLabel(stat.getBuildingType()), stat);
        }
        return grouped;
    }

    private Map<ResidenceBuildingType, AdminRentPrice> mapByBuildingType(List<AdminRentPrice> stats) {
        Map<ResidenceBuildingType, AdminRentPrice> mapped = new EnumMap<>(ResidenceBuildingType.class);
        for (AdminRentPrice stat : stats) {
            mapped.put(ResidenceBuildingType.fromLabel(stat.getBuildingType()), stat);
        }
        return mapped;
    }

    private AdminResidencePriceSummaryItemResponse buildSummary(
            AdminDong adminDong,
            Map<ResidenceBuildingType, AdminRentPrice> statsByBuildingType
    ) {
        ResidenceBuildingType dominantResidenceType = determineDominantResidenceType(statsByBuildingType);
        AdminRentPrice dominantStat = dominantResidenceType == null ? null : statsByBuildingType.get(dominantResidenceType);

        return new AdminResidencePriceSummaryItemResponse(
                adminDong.getAdminDongCode(),
                PERIOD_LABEL,
                toTypeResponse(dominantResidenceType),
                toSaleResponse(dominantStat),
                toJeonseResponse(dominantStat),
                toMonthlyRentResponse(dominantStat)
        );
    }

    private BuildingTypeResidencePriceResponse buildBuildingTypeResponse(
            ResidenceBuildingType buildingType,
            AdminRentPrice stat
    ) {
        return new BuildingTypeResidencePriceResponse(
                toTypeResponse(buildingType),
                toSaleResponse(stat),
                toJeonseResponse(stat),
                toMonthlyRentResponse(stat)
        );
    }

    private ResidenceBuildingType determineDominantResidenceType(
            Map<ResidenceBuildingType, AdminRentPrice> statsByBuildingType
    ) {
        ResidenceBuildingType dominantResidenceType = null;
        int dominantCount = 0;
        for (ResidenceBuildingType buildingType : ResidenceBuildingType.values()) {
            AdminRentPrice stat = statsByBuildingType.get(buildingType);
            int totalCount = totalCount(stat);
            if (totalCount > dominantCount) {
                dominantResidenceType = buildingType;
                dominantCount = totalCount;
            }
        }
        return dominantCount == 0 ? null : dominantResidenceType;
    }

    private int totalCount(AdminRentPrice stat) {
        if (stat == null) {
            return 0;
        }
        return safeCount(stat.getSaleCount()) + safeCount(stat.getJeonseCount()) + safeCount(stat.getMonthlyRentCount());
    }

    private int safeCount(Integer count) {
        return count == null ? 0 : count;
    }

    private ResidenceDisplayValueResponse toSaleResponse(AdminRentPrice stat) {
        if (stat == null) {
            return new ResidenceDisplayValueResponse(null);
        }
        return toDisplayValueResponse(rentPriceDisplayPolicy.decideSale(convertToRentPrice(stat)));
    }

    private ResidenceDisplayValueResponse toJeonseResponse(AdminRentPrice stat) {
        if (stat == null) {
            return new ResidenceDisplayValueResponse(null);
        }
        return toDisplayValueResponse(rentPriceDisplayPolicy.decideJeonse(convertToRentPrice(stat)));
    }

    private MonthlyRentDisplayValueResponse toMonthlyRentResponse(AdminRentPrice stat) {
        if (stat == null) {
            return new MonthlyRentDisplayValueResponse(null, null);
        }
        RentPriceDisplayPolicy.MonthlyRentDecision decision =
                rentPriceDisplayPolicy.decideMonthlyRent(convertToRentPrice(stat));
        if (decision.source() == RentPriceDisplayPolicy.MetricSource.NONE) {
            return new MonthlyRentDisplayValueResponse(null, null);
        }
        return new MonthlyRentDisplayValueResponse(
                decision.deposit(),
                decision.monthlyRent()
        );
    }

    private ResidenceDisplayValueResponse toDisplayValueResponse(RentPriceDisplayPolicy.PriceDecision decision) {
        if (decision.source() == RentPriceDisplayPolicy.MetricSource.NONE) {
            return new ResidenceDisplayValueResponse(null);
        }
        return new ResidenceDisplayValueResponse(decision.amount());
    }

    private ResidenceTypeResponse toTypeResponse(ResidenceBuildingType buildingType) {
        if (buildingType == null) {
            return null;
        }
        return new ResidenceTypeResponse(buildingType.code(), buildingType.label());
    }

    private RentPrice convertToRentPrice(AdminRentPrice stat) {
        return RentPrice.builder()
                .buildingType(stat.getBuildingType())
                .avgSalePrice(stat.getAvgSalePrice())
                .medianSalePrice(stat.getMedianSalePrice())
                .avgSalePricePerSquareMeter(stat.getAvgSalePricePerSquareMeter())
                .saleCount(stat.getSaleCount())
                .avgJeonseDeposit(stat.getAvgJeonseDeposit())
                .medianJeonseDeposit(stat.getMedianJeonseDeposit())
                .avgJeonseDepositPerSquareMeter(stat.getAvgJeonseDepositPerSquareMeter())
                .jeonseCount(stat.getJeonseCount())
                .avgMonthlyDeposit(stat.getAvgMonthlyDeposit())
                .medianMonthlyDeposit(stat.getMedianMonthlyDeposit())
                .avgMonthlyRent(stat.getAvgMonthlyRent())
                .medianMonthlyRent(stat.getMedianMonthlyRent())
                .monthlyRentCount(stat.getMonthlyRentCount())
                .build();
    }
}
