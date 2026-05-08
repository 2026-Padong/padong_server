package com.example.padong_server.domain.activityMobility.service;

import com.example.padong_server.domain.activityMobility.dto.ActivityMobilityCsvRow;
import com.example.padong_server.domain.activityMobility.dto.ActivityMobilityRepresentativeRow;
import com.example.padong_server.global.exception.ErrorCode;
import com.example.padong_server.global.util.Preconditions;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
public class ActivityMobilityAggregator {

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");
    private static final double COMMUTE_IN_WEIGHT = 0.8;
    private static final double COMMUTE_OUT_WEIGHT = 0.2;
    private static final double SOURCE_TOTAL_DIFF_LOG_THRESHOLD = 0.01;
    private static final String INVALID_PERIOD_MESSAGE_FORMAT =
            "startMonth must be before or equal to endMonth. startMonth=%s, endMonth=%s";
    private static final String ROW_MONTH_OUTSIDE_PERIOD_MESSAGE_FORMAT =
            "Activity mobility row month is outside aggregation period. month=%s, startMonth=%s,"
                    + " endMonth=%s";
    private static final String INVALID_MONTH_MESSAGE_FORMAT = "Invalid %s: %s. expected yyyyMM";

    public List<ActivityMobilityRepresentativeRow> aggregate(
            List<ActivityMobilityCsvRow> rows, String startMonth, String endMonth) {
        validatePeriod(startMonth, endMonth);
        int totalWeekdays = countWeekdays(startMonth, endMonth);
        Map<OdKey, Accumulator> accumulators = new LinkedHashMap<>();
        SourceTotalDiffStats sourceTotalDiffStats = new SourceTotalDiffStats();

        for (ActivityMobilityCsvRow row : rows) {
            validateRowInPeriod(row, startMonth, endMonth);
            OdKey key = new OdKey(row.arrivalDongCode(), row.departureDongCode());
            Accumulator accumulator =
                    accumulators.computeIfAbsent(key, ignored -> new Accumulator(row));
            accumulator.add(row);
            sourceTotalDiffStats.add(
                    row.sourceTotalMobility(), recalculateMonthlyTotalMobility(row));
        }

        logSourceTotalDiffStats(sourceTotalDiffStats);

        return accumulators.entrySet().stream()
                .map(
                        entry ->
                                entry.getValue()
                                        .toRepresentativeRow(startMonth, endMonth, totalWeekdays))
                .sorted(
                        (first, second) -> {
                            int arrivalCompare =
                                    first.arrivalDongCode().compareTo(second.arrivalDongCode());
                            if (arrivalCompare != 0) {
                                return arrivalCompare;
                            }
                            return first.departureDongCode().compareTo(second.departureDongCode());
                        })
                .toList();
    }

    int countWeekdays(String startMonth, String endMonth) {
        YearMonth start = parseMonth(startMonth, "startMonth");
        YearMonth end = parseMonth(endMonth, "endMonth");
        Preconditions.validate(!start.isAfter(end), ErrorCode.VALIDATION_ERROR);

        int count = 0;
        LocalDate date = start.atDay(1);
        LocalDate lastDate = end.atEndOfMonth();
        while (!date.isAfter(lastDate)) {
            DayOfWeek dayOfWeek = date.getDayOfWeek();
            if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY) {
                count++;
            }
            date = date.plusDays(1);
        }
        return count;
    }

    private void validatePeriod(String startMonth, String endMonth) {
        countWeekdays(startMonth, endMonth);
    }

    private void validateRowInPeriod(
            ActivityMobilityCsvRow row, String startMonth, String endMonth) {
        YearMonth rowMonth = parseMonth(row.month(), "row.month");
        YearMonth start = parseMonth(startMonth, "startMonth");
        YearMonth end = parseMonth(endMonth, "endMonth");
        Preconditions.validate(
                !rowMonth.isBefore(start) && !rowMonth.isAfter(end), ErrorCode.VALIDATION_ERROR);
    }

    private YearMonth parseMonth(String month, String label) {
        try {
            return YearMonth.parse(month, MONTH_FORMATTER);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(
                    INVALID_MONTH_MESSAGE_FORMAT.formatted(label, month), exception);
        }
    }

    private double recalculateMonthlyTotalMobility(ActivityMobilityCsvRow row) {
        return row.commuteInPopulation() * COMMUTE_IN_WEIGHT
                + row.commuteOutPopulation() * COMMUTE_OUT_WEIGHT;
    }

    private void logSourceTotalDiffStats(SourceTotalDiffStats stats) {
        if (stats.count == 0) {
            return;
        }
        log.info(
                "ActivityMobility source total comparison: rows={}, diffOverThreshold={},"
                        + " maxDiff={}",
                stats.count,
                stats.diffOverThresholdCount,
                stats.maxDiff);
    }

    private record OdKey(String arrivalDongCode, String departureDongCode) {}

    private class Accumulator {
        private final String arrivalDongCode;
        private final String departureDongCode;
        private final Set<String> observedMonths = new HashSet<>();
        private double commuteInPopulationSum;
        private double commuteOutPopulationSum;
        private double avgTimeWeightedSum;
        private double avgTimeWeightSum;

        private Accumulator(ActivityMobilityCsvRow firstRow) {
            this.arrivalDongCode = firstRow.arrivalDongCode();
            this.departureDongCode = firstRow.departureDongCode();
        }

        private void add(ActivityMobilityCsvRow row) {
            observedMonths.add(row.month());
            commuteInPopulationSum += row.commuteInPopulation();
            commuteOutPopulationSum += row.commuteOutPopulation();

            double monthlyTotalMobility = recalculateMonthlyTotalMobility(row);
            if (monthlyTotalMobility > 0) {
                avgTimeWeightedSum += row.avgTime() * monthlyTotalMobility;
                avgTimeWeightSum += monthlyTotalMobility;
            }
        }

        private ActivityMobilityRepresentativeRow toRepresentativeRow(
                String startMonth, String endMonth, int totalWeekdays) {
            double commuteInDailyAverage = commuteInPopulationSum / totalWeekdays;
            double commuteOutDailyAverage = commuteOutPopulationSum / totalWeekdays;
            double totalMobility =
                    commuteInDailyAverage * COMMUTE_IN_WEIGHT
                            + commuteOutDailyAverage * COMMUTE_OUT_WEIGHT;
            double avgTime = avgTimeWeightSum == 0 ? 0.0 : avgTimeWeightedSum / avgTimeWeightSum;

            return new ActivityMobilityRepresentativeRow(
                    startMonth,
                    endMonth,
                    arrivalDongCode,
                    departureDongCode,
                    totalMobility,
                    avgTime,
                    observedMonths.size());
        }
    }

    private static class SourceTotalDiffStats {
        private int count;
        private int diffOverThresholdCount;
        private double maxDiff;

        private void add(double sourceTotalMobility, double recalculatedTotalMobility) {
            count++;
            double diff = Math.abs(sourceTotalMobility - recalculatedTotalMobility);
            maxDiff = Math.max(maxDiff, diff);
            if (diff > SOURCE_TOTAL_DIFF_LOG_THRESHOLD) {
                diffOverThresholdCount++;
            }
        }
    }
}
