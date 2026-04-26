package com.example.padong_server.domain.rentPrice.service;

import com.example.padong_server.domain.dongne.entity.AdminDong;
import com.example.padong_server.domain.dongne.entity.DongMapping;
import com.example.padong_server.domain.dongne.entity.LegalDong;
import com.example.padong_server.domain.dongne.repository.DongMappingRepository;
import com.example.padong_server.domain.dongne.repository.LegalDongRepository;
import com.example.padong_server.domain.rentPrice.dto.internal.RentPriceRawData;
import com.example.padong_server.domain.rentPrice.dto.internal.RentPriceRawData.RentRow;
import com.example.padong_server.domain.rentPrice.dto.internal.RentPriceRawData.RentType;
import com.example.padong_server.domain.rentPrice.dto.internal.RentPriceRawData.SaleRow;
import com.example.padong_server.domain.rentPrice.dto.response.RentPriceImportResponse;
import com.example.padong_server.domain.rentPrice.entity.AdminRentPrice;
import com.example.padong_server.domain.rentPrice.entity.RentPrice;
import com.example.padong_server.domain.rentPrice.repository.AdminRentPriceRepository;
import com.example.padong_server.domain.rentPrice.repository.RentPriceRepository;
import com.example.padong_server.domain.rentPrice.util.RentPriceDataUtil;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RentPriceDataImportService {

    private static final int PER_SQUARE_METER_DIVIDE_SCALE = 6;

    private final RentPriceDataUtil rentPriceDataUtil;
    private final LegalDongRepository legalDongRepository;
    private final DongMappingRepository dongMappingRepository;
    private final RentPriceRepository rentPriceRepository;
    private final AdminRentPriceRepository adminRentPriceRepository;

    @Transactional
    public RentPriceImportResponse importData() {
        RentPriceRawData rawData = rentPriceDataUtil.readRows();
        MappingIndex mappingIndex = loadMappingIndex();

        Map<LegalStatKey, StatAccumulator> legalAccumulators = aggregateLegal(rawData);
        Map<AdminStatKey, StatAccumulator> adminAccumulators = aggregateAdmin(rawData, mappingIndex.adminDongsByLegalCode());

        List<RentPrice> legalStats = toLegalStats(legalAccumulators, loadLegalDongMap());
        List<AdminRentPrice> adminStats = toAdminStats(adminAccumulators, mappingIndex.adminByCode());

        adminRentPriceRepository.deleteAllInBatch();
        rentPriceRepository.deleteAllInBatch();
        rentPriceRepository.saveAll(legalStats);
        adminRentPriceRepository.saveAll(adminStats);

        return new RentPriceImportResponse(
                legalStats.size(),
                rawData.sourceRowCount(),
                rawData.saleRows().size(),
                rawData.rentRows().stream().filter(row -> row.rentType() == RentType.JEONSE).count(),
                rawData.rentRows().stream().filter(row -> row.rentType() == RentType.MONTHLY_RENT).count(),
                rawData.skippedRowCount()
        );
    }

    private MappingIndex loadMappingIndex() {
        Map<String, LinkedHashMap<String, AdminDong>> adminDongsByLegalCode = new HashMap<>();
        Map<String, AdminDong> adminByCode = new HashMap<>();

        for (DongMapping mapping : dongMappingRepository.findAllWithAdminAndLegal()) {
            AdminDong adminDong = mapping.getAdminDong();
            LegalDong legalDong = mapping.getLegalDong();

            adminByCode.put(adminDong.getAdminDongCode(), adminDong);
            adminDongsByLegalCode
                    .computeIfAbsent(legalDong.getLegalDongCode(), ignored -> new LinkedHashMap<>())
                    .put(adminDong.getAdminDongCode(), adminDong);
        }

        Map<String, List<AdminDong>> immutableMap = new HashMap<>();
        for (Map.Entry<String, LinkedHashMap<String, AdminDong>> entry : adminDongsByLegalCode.entrySet()) {
            immutableMap.put(entry.getKey(), List.copyOf(entry.getValue().values()));
        }

        return new MappingIndex(immutableMap, adminByCode);
    }

    private Map<LegalStatKey, StatAccumulator> aggregateLegal(RentPriceRawData rawData) {
        Map<LegalStatKey, StatAccumulator> accumulators = new HashMap<>();
        for (SaleRow row : rawData.saleRows()) {
            accumulatorForLegal(accumulators, row.legalDongCode(), row.buildingType())
                    .addSale(row.salePrice(), row.area());
        }
        for (RentRow row : rawData.rentRows()) {
            StatAccumulator accumulator = accumulatorForLegal(accumulators, row.legalDongCode(), row.buildingType());
            if (row.rentType() == RentType.JEONSE) {
                accumulator.addJeonse(row.deposit(), row.area());
                continue;
            }
            accumulator.addMonthlyRent(row.deposit(), row.monthlyRent());
        }
        return accumulators;
    }

    private Map<AdminStatKey, StatAccumulator> aggregateAdmin(
            RentPriceRawData rawData,
            Map<String, List<AdminDong>> adminDongsByLegalCode
    ) {
        Map<AdminStatKey, StatAccumulator> accumulators = new HashMap<>();
        for (SaleRow row : rawData.saleRows()) {
            for (AdminDong adminDong : requireAdminDongs(adminDongsByLegalCode, row.legalDongCode())) {
                accumulatorForAdmin(accumulators, adminDong.getAdminDongCode(), row.buildingType())
                        .addSale(row.salePrice(), row.area());
            }
        }
        for (RentRow row : rawData.rentRows()) {
            for (AdminDong adminDong : requireAdminDongs(adminDongsByLegalCode, row.legalDongCode())) {
                StatAccumulator accumulator = accumulatorForAdmin(
                        accumulators,
                        adminDong.getAdminDongCode(),
                        row.buildingType()
                );
                if (row.rentType() == RentType.JEONSE) {
                    accumulator.addJeonse(row.deposit(), row.area());
                    continue;
                }
                accumulator.addMonthlyRent(row.deposit(), row.monthlyRent());
            }
        }
        return accumulators;
    }

    private Map<String, LegalDong> loadLegalDongMap() {
        Map<String, LegalDong> legalDongMap = new HashMap<>();
        for (LegalDong legalDong : legalDongRepository.findAll()) {
            legalDongMap.put(legalDong.getLegalDongCode(), legalDong);
        }
        return legalDongMap;
    }

    private List<RentPrice> toLegalStats(
            Map<LegalStatKey, StatAccumulator> accumulators,
            Map<String, LegalDong> legalDongMap
    ) {
        List<RentPrice> stats = new ArrayList<>();
        List<LegalStatKey> keys = accumulators.keySet().stream()
                .sorted(Comparator.comparing(LegalStatKey::legalDongCode).thenComparing(LegalStatKey::buildingType))
                .toList();

        for (LegalStatKey key : keys) {
            LegalDong legalDong = legalDongMap.get(key.legalDongCode());
            if (legalDong == null) {
                throw new IllegalArgumentException("존재하지 않는 법정동 코드입니다: " + key.legalDongCode());
            }
            stats.add(buildLegalStat(key.buildingType(), legalDong, accumulators.get(key)));
        }
        return stats;
    }

    private List<AdminRentPrice> toAdminStats(
            Map<AdminStatKey, StatAccumulator> accumulators,
            Map<String, AdminDong> adminByCode
    ) {
        List<AdminRentPrice> stats = new ArrayList<>();
        List<AdminStatKey> keys = accumulators.keySet().stream()
                .sorted(Comparator.comparing(AdminStatKey::adminDongCode).thenComparing(AdminStatKey::buildingType))
                .toList();

        for (AdminStatKey key : keys) {
            AdminDong adminDong = adminByCode.get(key.adminDongCode());
            if (adminDong == null) {
                throw new IllegalArgumentException("존재하지 않는 행정동 코드입니다: " + key.adminDongCode());
            }
            stats.add(buildAdminStat(key.buildingType(), adminDong, accumulators.get(key)));
        }
        return stats;
    }

    private RentPrice buildLegalStat(
            String buildingType,
            LegalDong legalDong,
            StatAccumulator accumulator
    ) {
        MonthlyRentPair representativeMonthlyRentPair = accumulator.representativeMonthlyRentPair();
        return RentPrice.builder()
                .legalDong(legalDong)
                .buildingType(buildingType)
                .avgSalePrice(accumulator.salePrices.average())
                .medianSalePrice(accumulator.salePrices.median())
                .avgSalePricePerSquareMeter(accumulator.averageSalePricePerSquareMeter())
                .saleCount(accumulator.salePrices.count())
                .avgJeonseDeposit(accumulator.jeonseDeposits.average())
                .medianJeonseDeposit(accumulator.jeonseDeposits.median())
                .avgJeonseDepositPerSquareMeter(accumulator.averageJeonseDepositPerSquareMeter())
                .jeonseCount(accumulator.jeonseDeposits.count())
                .avgMonthlyDeposit(accumulator.monthlyDeposits.average())
                .medianMonthlyDeposit(depositOf(representativeMonthlyRentPair))
                .avgMonthlyRent(accumulator.monthlyRents.average())
                .medianMonthlyRent(monthlyRentOf(representativeMonthlyRentPair))
                .monthlyRentCount(accumulator.monthlyRents.count())
                .build();
    }

    private AdminRentPrice buildAdminStat(
            String buildingType,
            AdminDong adminDong,
            StatAccumulator accumulator
    ) {
        MonthlyRentPair representativeMonthlyRentPair = accumulator.representativeMonthlyRentPair();
        return AdminRentPrice.builder()
                .adminDong(adminDong)
                .buildingType(buildingType)
                .avgSalePrice(accumulator.salePrices.average())
                .medianSalePrice(accumulator.salePrices.median())
                .avgSalePricePerSquareMeter(accumulator.averageSalePricePerSquareMeter())
                .saleCount(accumulator.salePrices.count())
                .avgJeonseDeposit(accumulator.jeonseDeposits.average())
                .medianJeonseDeposit(accumulator.jeonseDeposits.median())
                .avgJeonseDepositPerSquareMeter(accumulator.averageJeonseDepositPerSquareMeter())
                .jeonseCount(accumulator.jeonseDeposits.count())
                .avgMonthlyDeposit(accumulator.monthlyDeposits.average())
                .medianMonthlyDeposit(depositOf(representativeMonthlyRentPair))
                .avgMonthlyRent(accumulator.monthlyRents.average())
                .medianMonthlyRent(monthlyRentOf(representativeMonthlyRentPair))
                .monthlyRentCount(accumulator.monthlyRents.count())
                .build();
    }

    private Long depositOf(MonthlyRentPair monthlyRentPair) {
        return monthlyRentPair == null ? null : monthlyRentPair.deposit();
    }

    private Long monthlyRentOf(MonthlyRentPair monthlyRentPair) {
        return monthlyRentPair == null ? null : monthlyRentPair.monthlyRent();
    }

    private List<AdminDong> requireAdminDongs(
            Map<String, List<AdminDong>> adminDongsByLegalCode,
            String legalDongCode
    ) {
        List<AdminDong> adminDongs = adminDongsByLegalCode.get(legalDongCode);
        if (adminDongs == null || adminDongs.isEmpty()) {
            throw new IllegalArgumentException("행정동 매핑이 없는 법정동 코드입니다: " + legalDongCode);
        }
        return adminDongs;
    }

    private StatAccumulator accumulatorForLegal(
            Map<LegalStatKey, StatAccumulator> accumulators,
            String legalDongCode,
            String buildingType
    ) {
        return accumulators.computeIfAbsent(
                new LegalStatKey(legalDongCode, buildingType),
                ignored -> new StatAccumulator()
        );
    }

    private StatAccumulator accumulatorForAdmin(
            Map<AdminStatKey, StatAccumulator> accumulators,
            String adminDongCode,
            String buildingType
    ) {
        return accumulators.computeIfAbsent(
                new AdminStatKey(adminDongCode, buildingType),
                ignored -> new StatAccumulator()
        );
    }

    private record LegalStatKey(String legalDongCode, String buildingType) {
    }

    private record AdminStatKey(String adminDongCode, String buildingType) {
    }

    private record MappingIndex(
            Map<String, List<AdminDong>> adminDongsByLegalCode,
            Map<String, AdminDong> adminByCode
    ) {
    }

    private record MonthlyRentPair(
            Long deposit,
            Long monthlyRent
    ) {
    }

    private static class StatAccumulator {
        private final LongValues salePrices = new LongValues();
        private final LongValues jeonseDeposits = new LongValues();
        private final LongValues monthlyDeposits = new LongValues();
        private final LongValues monthlyRents = new LongValues();
        private final List<MonthlyRentPair> monthlyRentPairs = new ArrayList<>();
        private BigDecimal salePricePerSquareMeterSum = BigDecimal.ZERO;
        private int salePricePerSquareMeterCount;
        private BigDecimal jeonseDepositPerSquareMeterSum = BigDecimal.ZERO;
        private int jeonseDepositPerSquareMeterCount;

        private void addSale(Long salePrice, BigDecimal area) {
            salePrices.add(salePrice);
            addSalePerSquareMeter(salePrice, area);
        }

        private void addJeonse(Long deposit, BigDecimal area) {
            jeonseDeposits.add(deposit);
            addJeonsePerSquareMeter(deposit, area);
        }

        private void addMonthlyRent(Long deposit, Long monthlyRent) {
            monthlyDeposits.add(deposit);
            monthlyRents.add(monthlyRent);
            monthlyRentPairs.add(new MonthlyRentPair(deposit, monthlyRent));
        }

        private void addSalePerSquareMeter(Long salePrice, BigDecimal area) {
            if (area == null) {
                return;
            }
            salePricePerSquareMeterSum = salePricePerSquareMeterSum.add(perSquareMeter(salePrice, area));
            salePricePerSquareMeterCount++;
        }

        private void addJeonsePerSquareMeter(Long deposit, BigDecimal area) {
            if (area == null) {
                return;
            }
            jeonseDepositPerSquareMeterSum = jeonseDepositPerSquareMeterSum.add(perSquareMeter(deposit, area));
            jeonseDepositPerSquareMeterCount++;
        }

        private BigDecimal averageSalePricePerSquareMeter() {
            return averageDecimal(salePricePerSquareMeterSum, salePricePerSquareMeterCount);
        }

        private BigDecimal averageJeonseDepositPerSquareMeter() {
            return averageDecimal(jeonseDepositPerSquareMeterSum, jeonseDepositPerSquareMeterCount);
        }

        private BigDecimal perSquareMeter(Long price, BigDecimal area) {
            return BigDecimal.valueOf(price).divide(area, PER_SQUARE_METER_DIVIDE_SCALE, RoundingMode.HALF_UP);
        }

        private BigDecimal averageDecimal(BigDecimal sum, int count) {
            if (count == 0) {
                return null;
            }
            return sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
        }

        private MonthlyRentPair representativeMonthlyRentPair() {
            if (monthlyRentPairs.isEmpty()) {
                return null;
            }
            Long medianDeposit = monthlyDeposits.median();
            Long medianMonthlyRent = monthlyRents.median();
            return monthlyRentPairs.stream()
                    .min(Comparator
                            .comparingDouble((MonthlyRentPair pair) -> representativeScore(
                                    pair,
                                    medianDeposit,
                                    medianMonthlyRent
                            ))
                            .thenComparingDouble(pair -> normalizedDistance(pair.monthlyRent(), medianMonthlyRent))
                            .thenComparingDouble(pair -> normalizedDistance(pair.deposit(), medianDeposit))
                            .thenComparing(MonthlyRentPair::monthlyRent)
                            .thenComparing(MonthlyRentPair::deposit))
                    .orElse(null);
        }

        private double representativeScore(
                MonthlyRentPair monthlyRentPair,
                Long medianDeposit,
                Long medianMonthlyRent
        ) {
            return normalizedDistance(monthlyRentPair.deposit(), medianDeposit)
                    + normalizedDistance(monthlyRentPair.monthlyRent(), medianMonthlyRent);
        }

        private double normalizedDistance(Long value, Long baseline) {
            if (value == null || baseline == null) {
                return 0.0;
            }
            long distance = Math.abs(value - baseline);
            if (baseline == 0L) {
                return distance;
            }
            return (double) distance / Math.abs(baseline);
        }
    }

    private static class LongValues {
        private final List<Long> values = new ArrayList<>();

        private void add(Long value) {
            values.add(value);
        }

        private Integer count() {
            return values.size();
        }

        private Long average() {
            if (values.isEmpty()) {
                return null;
            }
            long sum = 0L;
            for (Long value : values) {
                sum += value;
            }
            return BigDecimal.valueOf(sum)
                    .divide(BigDecimal.valueOf(values.size()), 0, RoundingMode.HALF_UP)
                    .longValue();
        }

        private Long median() {
            if (values.isEmpty()) {
                return null;
            }
            List<Long> sorted = values.stream().sorted().toList();
            int middle = sorted.size() / 2;
            if (sorted.size() % 2 == 1) {
                return sorted.get(middle);
            }
            return BigDecimal.valueOf(sorted.get(middle - 1))
                    .add(BigDecimal.valueOf(sorted.get(middle)))
                    .divide(BigDecimal.valueOf(2), 0, RoundingMode.HALF_UP)
                    .longValue();
        }
    }
}
