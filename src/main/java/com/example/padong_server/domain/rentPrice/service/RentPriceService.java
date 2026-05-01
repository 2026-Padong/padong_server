package com.example.padong_server.domain.rentPrice.service;

import com.example.padong_server.domain.dongne.entity.AdminDong;
import com.example.padong_server.domain.dongne.repository.AdminDongRepository;
import com.example.padong_server.domain.rentPrice.dto.response.AdminDongRentPriceDetailResponse;
import com.example.padong_server.domain.rentPrice.dto.response.AdminDongRentPriceSummaryResponse;
import com.example.padong_server.domain.rentPrice.dto.response.AdminDongRentPriceBuildingTypeResponse;
import com.example.padong_server.domain.rentPrice.dto.response.MonthlyRentDisplayValueResponse;
import com.example.padong_server.domain.rentPrice.dto.response.RentPriceDisplayValueResponse;
import com.example.padong_server.domain.rentPrice.dto.response.RentPriceTradeTypeResponse;
import com.example.padong_server.domain.rentPrice.dto.response.ResidenceBuildingTypeResponse;
import com.example.padong_server.domain.rentPrice.entity.AdminRentPrice;
import com.example.padong_server.domain.rentPrice.entity.ResidenceBuildingType;
import com.example.padong_server.domain.rentPrice.entity.RentPrice;
import com.example.padong_server.domain.rentPrice.entity.RentPriceTradeType;
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

    public List<AdminDongRentPriceSummaryResponse> getSummaries(
            List<String> requestedAdminDongCodes,
            String buildingTypeLabel,
            String tradeTypeLabel
    ) {
        if (requestedAdminDongCodes == null) {
            throw new IllegalArgumentException("행정동 코드는 비어 있을 수 없습니다.");
        }
        ResidenceBuildingType buildingType = selectedBuildingType(buildingTypeLabel);
        RentPriceTradeType tradeType = selectedTradeType(tradeTypeLabel);
        List<String> adminDongCodes = sanitizeAdminDongCodes(requestedAdminDongCodes);
        if (adminDongCodes.isEmpty()) {
            return List.of();
        }

        Map<String, AdminDong> adminDongByCode = loadAdminDongByCode(adminDongCodes);
        Map<String, Map<ResidenceBuildingType, AdminRentPrice>> statsByAdminDongCode =
                groupByAdminDongCode(adminRentPriceRepository.findAllByAdminDongAdminDongCodeIn(adminDongCodes));

        List<AdminDongRentPriceSummaryResponse> responses = new ArrayList<>();
        for (String adminDongCode : adminDongCodes) {
            AdminDong adminDong = adminDongByCode.get(adminDongCode);
            responses.add(buildFilteredSummary(
                    adminDong,
                    buildingType,
                    tradeType,
                    statsByAdminDongCode.getOrDefault(adminDongCode, Map.of())
            ));
        }
        return responses;
    }

    private ResidenceBuildingType selectedBuildingType(String buildingTypeLabel) {
        if (buildingTypeLabel == null || buildingTypeLabel.trim().isEmpty()) {
            return ResidenceBuildingType.DETACHED_MULTIFAMILY;
        }
        return ResidenceBuildingType.from(buildingTypeLabel);
    }

    private RentPriceTradeType selectedTradeType(String tradeTypeLabel) {
        if (tradeTypeLabel == null || tradeTypeLabel.trim().isEmpty()) {
            return RentPriceTradeType.MONTHLY_RENT;
        }
        return RentPriceTradeType.from(tradeTypeLabel);
    }

    public AdminDongRentPriceDetailResponse getDetail(String adminDongCode) {
        String sanitizedCode = sanitizeAdminDongCode(adminDongCode);
        AdminDong adminDong = adminDongRepository.findByAdminDongCode(sanitizedCode)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 행정동 코드입니다: " + sanitizedCode));

        Map<ResidenceBuildingType, AdminRentPrice> statsByBuildingType =
                mapByBuildingType(adminRentPriceRepository.findAllByAdminDongAdminDongCode(sanitizedCode));

        return buildDetailResponse(adminDong, statsByBuildingType);
    }

    public List<AdminDongRentPriceDetailResponse> getDetails(List<String> requestedAdminDongCodes) {
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

        List<AdminDongRentPriceDetailResponse> responses = new ArrayList<>();
        for (String adminDongCode : adminDongCodes) {
            responses.add(buildDetailResponse(
                    adminDongByCode.get(adminDongCode),
                    statsByAdminDongCode.getOrDefault(adminDongCode, Map.of())
            ));
        }
        return responses;
    }

    private AdminDongRentPriceDetailResponse buildDetailResponse(
            AdminDong adminDong,
            Map<ResidenceBuildingType, AdminRentPrice> statsByBuildingType
    ) {
        List<AdminDongRentPriceBuildingTypeResponse> buildingTypes = new ArrayList<>();
        for (ResidenceBuildingType buildingType : ResidenceBuildingType.values()) {
            AdminDongRentPriceBuildingTypeResponse response = buildBuildingTypeResponse(
                    buildingType,
                    statsByBuildingType.get(buildingType)
            );
            buildingTypes.add(response);
        }

        ResidenceBuildingType dominantBuildingType = determineDominantBuildingType(statsByBuildingType);
        return new AdminDongRentPriceDetailResponse(
                adminDong.getAdminDongCode(),
                PERIOD_LABEL,
                CONTRACT_PERIOD_START,
                CONTRACT_PERIOD_END,
                true,
                toTypeResponse(dominantBuildingType),
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

    private AdminDongRentPriceSummaryResponse buildFilteredSummary(
            AdminDong adminDong,
            ResidenceBuildingType buildingType,
            RentPriceTradeType tradeType,
            Map<ResidenceBuildingType, AdminRentPrice> statsByBuildingType
    ) {
        AdminRentPrice selectedStat = statsByBuildingType.get(buildingType);
        return new AdminDongRentPriceSummaryResponse(
                adminDong.getAdminDongCode(),
                PERIOD_LABEL,
                toTypeResponse(buildingType),
                toTradeTypeResponse(tradeType),
                tradeType == RentPriceTradeType.SALE ? toSaleResponse(selectedStat) : new RentPriceDisplayValueResponse(null),
                tradeType == RentPriceTradeType.JEONSE ? toJeonseResponse(selectedStat) : new RentPriceDisplayValueResponse(null),
                tradeType == RentPriceTradeType.MONTHLY_RENT
                        ? toMonthlyRentResponse(selectedStat)
                        : new MonthlyRentDisplayValueResponse(null, null)
        );
    }

    private AdminDongRentPriceBuildingTypeResponse buildBuildingTypeResponse(
            ResidenceBuildingType buildingType,
            AdminRentPrice stat
    ) {
        return new AdminDongRentPriceBuildingTypeResponse(
                toTypeResponse(buildingType),
                toSaleResponse(stat),
                toJeonseResponse(stat),
                toMonthlyRentResponse(stat)
        );
    }

    private ResidenceBuildingType determineDominantBuildingType(
            Map<ResidenceBuildingType, AdminRentPrice> statsByBuildingType
    ) {
        ResidenceBuildingType dominantBuildingType = null;
        int dominantCount = 0;
        for (ResidenceBuildingType buildingType : ResidenceBuildingType.values()) {
            AdminRentPrice stat = statsByBuildingType.get(buildingType);
            int totalCount = totalCount(stat);
            if (totalCount > dominantCount) {
                dominantBuildingType = buildingType;
                dominantCount = totalCount;
            }
        }
        return dominantCount == 0 ? null : dominantBuildingType;
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

    private RentPriceDisplayValueResponse toSaleResponse(AdminRentPrice stat) {
        if (stat == null) {
            return new RentPriceDisplayValueResponse(null);
        }
        return toDisplayValueResponse(rentPriceDisplayPolicy.decideSale(convertToRentPrice(stat)));
    }

    private RentPriceDisplayValueResponse toJeonseResponse(AdminRentPrice stat) {
        if (stat == null) {
            return new RentPriceDisplayValueResponse(null);
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

    private RentPriceDisplayValueResponse toDisplayValueResponse(RentPriceDisplayPolicy.PriceDecision decision) {
        if (decision.source() == RentPriceDisplayPolicy.MetricSource.NONE) {
            return new RentPriceDisplayValueResponse(null);
        }
        return new RentPriceDisplayValueResponse(decision.amount());
    }

    private RentPriceTradeTypeResponse toTradeTypeResponse(RentPriceTradeType tradeType) {
        return new RentPriceTradeTypeResponse(tradeType.code(), tradeType.label());
    }

    private ResidenceBuildingTypeResponse toTypeResponse(ResidenceBuildingType buildingType) {
        if (buildingType == null) {
            return null;
        }
        return new ResidenceBuildingTypeResponse(buildingType.code(), buildingType.label());
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
