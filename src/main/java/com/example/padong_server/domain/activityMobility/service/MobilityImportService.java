package com.example.padong_server.domain.activityMobility.service;

import com.example.padong_server.domain.activityMobility.dto.ActivityMobilityCsvRow;
import com.example.padong_server.domain.activityMobility.dto.ActivityMobilityRepresentativeRow;
import com.example.padong_server.domain.activityMobility.entity.Mobility;
import com.example.padong_server.domain.activityMobility.repository.MobilityRepository;
import com.example.padong_server.domain.activityMobility.util.ActivityMobilityDataUtil;
import com.example.padong_server.global.util.Preconditions;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MobilityImportService {

    private static final String EMPTY_ACTIVITY_MOBILITY_CSV_ROW_MESSAGE = "생활이동 CSV row가 없습니다.";

    private final ActivityMobilityDataUtil activityMobilityDataUtil;
    private final ActivityMobilityAggregator activityMobilityAggregator;
    private final ActivityMobilityEntityMapper activityMobilityEntityMapper;
    private final MobilityRepository mobilityRepository;

    @Transactional
    public String importData() {
        long start = System.currentTimeMillis();
        List<ActivityMobilityCsvRow> csvRows = activityMobilityDataUtil.readMonthlyCsvRows();
        ImportPeriod period = resolveImportPeriod(csvRows);
        List<ActivityMobilityRepresentativeRow> representativeRows =
                activityMobilityAggregator.aggregate(
                        csvRows, period.startMonth(), period.endMonth());
        List<Mobility> mobilities = activityMobilityEntityMapper.toEntities(representativeRows);

        mobilityRepository.deleteAllInBatch();
        List<Mobility> savedMobilities = mobilityRepository.saveAll(mobilities);

        long end = System.currentTimeMillis();
        log.info(
                "ActivityMobility import completed: period={}~{}, sourceRows={},"
                        + " representativeRows={}, savedRows={}, elapsedMs={}",
                period.startMonth(),
                period.endMonth(),
                csvRows.size(),
                representativeRows.size(),
                savedMobilities.size(),
                end - start);
        return "생활이동 데이터 적재 완료: "
                + period.startMonth()
                + "~"
                + period.endMonth()
                + " 기준, "
                + savedMobilities.size()
                + "건 저장";
    }

    private ImportPeriod resolveImportPeriod(List<ActivityMobilityCsvRow> rows) {
        Preconditions.validate(!rows.isEmpty(), EMPTY_ACTIVITY_MOBILITY_CSV_ROW_MESSAGE);
        String startMonth =
                rows.stream()
                        .map(ActivityMobilityCsvRow::month)
                        .min(Comparator.naturalOrder())
                        .orElseThrow();
        String endMonth =
                rows.stream()
                        .map(ActivityMobilityCsvRow::month)
                        .max(Comparator.naturalOrder())
                        .orElseThrow();
        return new ImportPeriod(startMonth, endMonth);
    }

    private record ImportPeriod(String startMonth, String endMonth) {}
}
