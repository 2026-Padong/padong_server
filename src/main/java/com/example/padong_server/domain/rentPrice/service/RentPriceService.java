package com.example.padong_server.domain.rentPrice.service;

import com.example.padong_server.domain.dongne.entity.AdminDong;
import com.example.padong_server.domain.dongne.repository.AdminDongRepository;
import com.example.padong_server.domain.rentPrice.dto.request.RentPriceFilterCriteria;
import com.example.padong_server.domain.rentPrice.dto.response.AdminDongRentPriceBuildingTypeResponse;
import com.example.padong_server.domain.rentPrice.dto.response.AdminDongRentPriceDetailResponse;
import com.example.padong_server.domain.rentPrice.dto.response.AdminDongRentPriceSummaryResponse;
import com.example.padong_server.domain.rentPrice.dto.response.MonthlyRentDisplayValueResponse;
import com.example.padong_server.domain.rentPrice.dto.response.RentPriceDisplayValueResponse;
import com.example.padong_server.domain.rentPrice.dto.response.RentPriceTradeTypeResponse;
import com.example.padong_server.domain.rentPrice.dto.response.ResidenceBuildingTypeResponse;
import com.example.padong_server.domain.rentPrice.dto.response.SelectedRentPriceResponse;
import com.example.padong_server.domain.rentPrice.entity.RentPrice;
import com.example.padong_server.domain.rentPrice.entity.RentPriceTradeType;
import com.example.padong_server.domain.rentPrice.entity.ResidenceBuildingType;
import com.example.padong_server.domain.rentPrice.policy.RentPriceDisplayPolicy;
import com.example.padong_server.domain.rentPrice.repository.RentPriceRepository;
import com.example.padong_server.global.exception.ErrorCode;
import com.example.padong_server.global.util.Preconditions;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RentPriceService {

    private static final String PERIOD_LABEL = "최근 2년 기준";
    private static final String CONTRACT_PERIOD_START = "2024-04-18";
    private static final String CONTRACT_PERIOD_END = "2026-04-17";
    private static final String RENT_PRICE_FILTER_REQUIRED_MESSAGE = "가격 필터 조건은 비어 있을 수 없습니다.";

    private final AdminDongRepository adminDongRepository;
    private final RentPriceRepository rentPriceRepository;
    private final RentPriceDisplayPolicy rentPriceDisplayPolicy;

    public List<AdminDongRentPriceSummaryResponse> getSummaries(
            List<String> requestedAdminDongCodes, String buildingTypeLabel, String tradeTypeLabel) {
        Preconditions.validate(requestedAdminDongCodes != null, ErrorCode.VALIDATION_ERROR);
        ResidenceBuildingType buildingType = selectedBuildingType(buildingTypeLabel);
        RentPriceTradeType tradeType = selectedTradeType(tradeTypeLabel);
        List<String> adminDongCodes = sanitizeAdminDongCodes(requestedAdminDongCodes);
        if (adminDongCodes.isEmpty()) {
            return List.of();
        }

        Map<String, AdminDong> adminDongByCode = loadAdminDongByCode(adminDongCodes);
        Map<String, Map<ResidenceBuildingType, RentPrice>> statsByAdminDongCode =
                groupByAdminDongCode(
                        rentPriceRepository.findAllByAdminDongAdminDongCodeIn(adminDongCodes));

        List<AdminDongRentPriceSummaryResponse> responses = new ArrayList<>();
        for (String adminDongCode : adminDongCodes) {
            AdminDong adminDong = adminDongByCode.get(adminDongCode);
            responses.add(
                    buildFilteredSummary(
                            adminDong,
                            buildingType,
                            tradeType,
                            statsByAdminDongCode.getOrDefault(adminDongCode, Map.of())));
        }
        return responses;
    }

    private ResidenceBuildingType selectedBuildingType(String buildingTypeLabel) {
        return ResidenceBuildingType.fromNullableOrDefault(
                buildingTypeLabel, RentPriceFilterCriteria.DEFAULT_BUILDING_TYPE);
    }

    private RentPriceTradeType selectedTradeType(String tradeTypeLabel) {
        return RentPriceTradeType.fromNullableOrDefault(
                tradeTypeLabel, RentPriceFilterCriteria.DEFAULT_TRADE_TYPE);
    }

    public AdminDongRentPriceDetailResponse getDetail(String adminDongCode) {
        AdminDong adminDong = adminDongRepository.getByAdminDongCode(adminDongCode);
        String sanitizedCode = adminDong.getAdminDongCode();

        Map<ResidenceBuildingType, RentPrice> statsByBuildingType =
                mapByBuildingType(
                        rentPriceRepository.findAllByAdminDongAdminDongCode(sanitizedCode));

        return buildDetailResponse(adminDong, statsByBuildingType);
    }

    public List<AdminDongRentPriceDetailResponse> getDetails(List<String> requestedAdminDongCodes) {
        Preconditions.validate(requestedAdminDongCodes != null, ErrorCode.VALIDATION_ERROR);
        List<String> adminDongCodes = sanitizeAdminDongCodes(requestedAdminDongCodes);
        if (adminDongCodes.isEmpty()) {
            return List.of();
        }

        Map<String, AdminDong> adminDongByCode = loadAdminDongByCode(adminDongCodes);
        Map<String, Map<ResidenceBuildingType, RentPrice>> statsByAdminDongCode =
                groupByAdminDongCode(
                        rentPriceRepository.findAllByAdminDongAdminDongCodeIn(adminDongCodes));

        List<AdminDongRentPriceDetailResponse> responses = new ArrayList<>();
        for (String adminDongCode : adminDongCodes) {
            responses.add(
                    buildDetailResponse(
                            adminDongByCode.get(adminDongCode),
                            statsByAdminDongCode.getOrDefault(adminDongCode, Map.of())));
        }
        return responses;
    }

    public Map<String, SelectedRentPriceResponse> getSelectedRentPrices(
            List<String> requestedAdminDongCodes, RentPriceFilterCriteria criteria) {
        Preconditions.validate(criteria != null, ErrorCode.VALIDATION_ERROR);
        Map<String, SelectedRentPriceResponse> rentPriceByAdminDongCode = new LinkedHashMap<>();
        for (AdminDongRentPriceDetailResponse detail : getDetails(requestedAdminDongCodes)) {
            SelectedRentPriceResponse rentPrice = toSelectedRentPriceResponse(detail, criteria);
            if (rentPrice != null) {
                rentPriceByAdminDongCode.put(detail.adminDongCode(), rentPrice);
            }
        }
        return rentPriceByAdminDongCode;
    }

    public Set<String> findMatchedAdminDongCodes(
            List<String> requestedAdminDongCodes, RentPriceFilterCriteria criteria) {
        Preconditions.validate(criteria != null, ErrorCode.VALIDATION_ERROR);
        Set<String> matchedAdminDongCodes = new LinkedHashSet<>();
        for (AdminDongRentPriceDetailResponse detail : getDetails(requestedAdminDongCodes)) {
            if (matchesRentPrice(detail, criteria)) {
                matchedAdminDongCodes.add(detail.adminDongCode());
            }
        }
        return matchedAdminDongCodes;
    }

    private SelectedRentPriceResponse toSelectedRentPriceResponse(
            AdminDongRentPriceDetailResponse detail, RentPriceFilterCriteria criteria) {
        if (detail == null || detail.buildingTypes() == null) {
            return null;
        }
        AdminDongRentPriceBuildingTypeResponse selectedBuildingType =
                selectBuildingTypeResponse(detail, criteria.buildingType());
        if (selectedBuildingType == null) {
            return null;
        }
        return SelectedRentPriceResponse.from(selectedBuildingType, criteria.tradeType());
    }

    private AdminDongRentPriceBuildingTypeResponse selectBuildingTypeResponse(
            AdminDongRentPriceDetailResponse detail, ResidenceBuildingType buildingType) {
        return detail.buildingTypes().stream()
                .filter(response -> matchesHouseType(response, buildingType))
                .findFirst()
                .orElse(null);
    }

    private boolean matchesRentPrice(
            AdminDongRentPriceDetailResponse detail, RentPriceFilterCriteria criteria) {
        if (detail == null || detail.buildingTypes() == null) {
            return false;
        }
        return detail.buildingTypes().stream()
                .filter(response -> matchesHouseType(response, criteria.buildingType()))
                .anyMatch(response -> matchesContractTypeAndPrice(response, criteria));
    }

    private boolean matchesHouseType(
            AdminDongRentPriceBuildingTypeResponse response, ResidenceBuildingType houseType) {
        return response.buildingType() != null
                && houseType.code().equals(response.buildingType().buildingTypeCode());
    }

    private boolean matchesContractTypeAndPrice(
            AdminDongRentPriceBuildingTypeResponse response, RentPriceFilterCriteria criteria) {
        return switch (criteria.tradeType()) {
            case SALE -> matchesSalePrice(response.sale(), criteria);
            case JEONSE -> matchesJeonseDeposit(response.jeonse(), criteria);
            case MONTHLY_RENT -> matchesMonthlyRent(response.monthlyRent(), criteria);
        };
    }

    private boolean matchesSalePrice(
            RentPriceDisplayValueResponse sale, RentPriceFilterCriteria criteria) {
        return sale != null
                && matchesMoneyRange(
                        sale.amount(), criteria.minSalePrice(), criteria.maxSalePrice());
    }

    private boolean matchesJeonseDeposit(
            RentPriceDisplayValueResponse jeonse, RentPriceFilterCriteria criteria) {
        return jeonse != null
                && matchesMoneyRange(
                        jeonse.amount(), criteria.minJeonseDeposit(), criteria.maxJeonseDeposit());
    }

    private boolean matchesMonthlyRent(
            MonthlyRentDisplayValueResponse monthlyRent, RentPriceFilterCriteria criteria) {
        return monthlyRent != null
                && matchesMoneyRange(
                        monthlyRent.deposit(),
                        criteria.minMonthlyDeposit(),
                        criteria.maxMonthlyDeposit())
                && matchesMoneyRange(
                        monthlyRent.monthlyRent(),
                        criteria.minMonthlyRent(),
                        criteria.maxMonthlyRent());
    }

    private boolean matchesMoneyRange(Long amountInManwon, Long minManwon, Long maxManwon) {
        if (amountInManwon == null) {
            return false;
        }
        if (minManwon != null && amountInManwon < minManwon) {
            return false;
        }
        return maxManwon == null || amountInManwon <= maxManwon;
    }

    private AdminDongRentPriceDetailResponse buildDetailResponse(
            AdminDong adminDong, Map<ResidenceBuildingType, RentPrice> statsByBuildingType) {
        List<AdminDongRentPriceBuildingTypeResponse> buildingTypes = new ArrayList<>();
        for (ResidenceBuildingType buildingType : ResidenceBuildingType.values()) {
            AdminDongRentPriceBuildingTypeResponse response =
                    buildBuildingTypeResponse(buildingType, statsByBuildingType.get(buildingType));
            buildingTypes.add(response);
        }

        ResidenceBuildingType dominantBuildingType =
                determineDominantBuildingType(statsByBuildingType);
        return new AdminDongRentPriceDetailResponse(
                adminDong.getAdminDongCode(),
                PERIOD_LABEL,
                CONTRACT_PERIOD_START,
                CONTRACT_PERIOD_END,
                true,
                toTypeResponse(dominantBuildingType),
                List.copyOf(buildingTypes));
    }

    private List<String> sanitizeAdminDongCodes(List<String> requestedAdminDongCodes) {
        List<String> adminDongCodes = new ArrayList<>();
        for (String adminDongCode : requestedAdminDongCodes) {
            adminDongCodes.add(sanitizeAdminDongCode(adminDongCode));
        }
        return adminDongCodes;
    }

    private String sanitizeAdminDongCode(String adminDongCode) {
        Preconditions.validate(
                adminDongCode != null && !adminDongCode.trim().isEmpty(),
                ErrorCode.VALIDATION_ERROR);
        return adminDongCode.trim();
    }

    private Map<String, AdminDong> loadAdminDongByCode(Collection<String> adminDongCodes) {
        Map<String, AdminDong> adminDongByCode = new LinkedHashMap<>();
        for (AdminDong adminDong : adminDongRepository.findAllByAdminDongCodeIn(adminDongCodes)) {
            adminDongByCode.put(adminDong.getAdminDongCode(), adminDong);
        }
        for (String adminDongCode : adminDongCodes) {
            Preconditions.validate(
                    adminDongByCode.containsKey(adminDongCode), ErrorCode.VALIDATION_ERROR);
        }
        return adminDongByCode;
    }

    private Map<String, Map<ResidenceBuildingType, RentPrice>> groupByAdminDongCode(
            List<RentPrice> stats) {
        Map<String, Map<ResidenceBuildingType, RentPrice>> grouped = new HashMap<>();
        for (RentPrice stat : stats) {
            grouped.computeIfAbsent(
                            stat.getAdminDong().getAdminDongCode(),
                            ignored -> new EnumMap<>(ResidenceBuildingType.class))
                    .put(ResidenceBuildingType.fromLabel(stat.getBuildingType()), stat);
        }
        return grouped;
    }

    private Map<ResidenceBuildingType, RentPrice> mapByBuildingType(List<RentPrice> stats) {
        Map<ResidenceBuildingType, RentPrice> mapped = new EnumMap<>(ResidenceBuildingType.class);
        for (RentPrice stat : stats) {
            mapped.put(ResidenceBuildingType.fromLabel(stat.getBuildingType()), stat);
        }
        return mapped;
    }

    private AdminDongRentPriceSummaryResponse buildFilteredSummary(
            AdminDong adminDong,
            ResidenceBuildingType buildingType,
            RentPriceTradeType tradeType,
            Map<ResidenceBuildingType, RentPrice> statsByBuildingType) {
        RentPrice selectedStat = statsByBuildingType.get(buildingType);
        return new AdminDongRentPriceSummaryResponse(
                adminDong.getAdminDongCode(),
                PERIOD_LABEL,
                toTypeResponse(buildingType),
                toTradeTypeResponse(tradeType),
                tradeType == RentPriceTradeType.SALE
                        ? toSaleResponse(selectedStat)
                        : new RentPriceDisplayValueResponse(null),
                tradeType == RentPriceTradeType.JEONSE
                        ? toJeonseResponse(selectedStat)
                        : new RentPriceDisplayValueResponse(null),
                tradeType == RentPriceTradeType.MONTHLY_RENT
                        ? toMonthlyRentResponse(selectedStat)
                        : new MonthlyRentDisplayValueResponse(null, null));
    }

    private AdminDongRentPriceBuildingTypeResponse buildBuildingTypeResponse(
            ResidenceBuildingType buildingType, RentPrice stat) {
        return new AdminDongRentPriceBuildingTypeResponse(
                toTypeResponse(buildingType),
                toSaleResponse(stat),
                toJeonseResponse(stat),
                toMonthlyRentResponse(stat));
    }

    private ResidenceBuildingType determineDominantBuildingType(
            Map<ResidenceBuildingType, RentPrice> statsByBuildingType) {
        ResidenceBuildingType dominantBuildingType = null;
        int dominantCount = 0;
        for (ResidenceBuildingType buildingType : ResidenceBuildingType.values()) {
            RentPrice stat = statsByBuildingType.get(buildingType);
            int totalCount = totalCount(stat);
            if (totalCount > dominantCount) {
                dominantBuildingType = buildingType;
                dominantCount = totalCount;
            }
        }
        return dominantCount == 0 ? null : dominantBuildingType;
    }

    private int totalCount(RentPrice stat) {
        if (stat == null) {
            return 0;
        }
        return safeCount(stat.getSaleCount())
                + safeCount(stat.getJeonseCount())
                + safeCount(stat.getMonthlyRentCount());
    }

    private int safeCount(Integer count) {
        return count == null ? 0 : count;
    }

    private RentPriceDisplayValueResponse toSaleResponse(RentPrice stat) {
        if (stat == null) {
            return new RentPriceDisplayValueResponse(null);
        }
        return toDisplayValueResponse(rentPriceDisplayPolicy.decideSale(stat));
    }

    private RentPriceDisplayValueResponse toJeonseResponse(RentPrice stat) {
        if (stat == null) {
            return new RentPriceDisplayValueResponse(null);
        }
        return toDisplayValueResponse(rentPriceDisplayPolicy.decideJeonse(stat));
    }

    private MonthlyRentDisplayValueResponse toMonthlyRentResponse(RentPrice stat) {
        if (stat == null) {
            return new MonthlyRentDisplayValueResponse(null, null);
        }
        RentPriceDisplayPolicy.MonthlyRentDecision decision =
                rentPriceDisplayPolicy.decideMonthlyRent(stat);
        if (decision.source() == RentPriceDisplayPolicy.MetricSource.NONE) {
            return new MonthlyRentDisplayValueResponse(null, null);
        }
        return new MonthlyRentDisplayValueResponse(decision.deposit(), decision.monthlyRent());
    }

    private RentPriceDisplayValueResponse toDisplayValueResponse(
            RentPriceDisplayPolicy.PriceDecision decision) {
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
}
