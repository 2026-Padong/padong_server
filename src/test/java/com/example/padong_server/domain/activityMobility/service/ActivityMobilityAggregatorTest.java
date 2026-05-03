package com.example.padong_server.domain.activityMobility.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import com.example.padong_server.domain.activityMobility.dto.ActivityMobilityCsvRow;
import com.example.padong_server.domain.activityMobility.dto.ActivityMobilityRepresentativeRow;
import com.example.padong_server.global.exception.CustomException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

class ActivityMobilityAggregatorTest {

    private final ActivityMobilityAggregator aggregator = new ActivityMobilityAggregator();

    @Test
    @DisplayName("월별 row를 OD 기준으로 묶어 최근 3개월 대표값을 계산한다")
    void aggregatesRowsToRepresentativeRows() {
        List<ActivityMobilityCsvRow> rows =
                List.of(
                        row("202601", "1113075", "1121058", 198.0, 220.0, 110.0, 10.0),
                        row("202603", "1113075", "1121058", 198.0, 220.0, 110.0, 30.0),
                        row("202602", "1113075", "1113075", 90.0, 100.0, 50.0, 40.0));

        List<ActivityMobilityRepresentativeRow> result =
                aggregator.aggregate(rows, "202601", "202603");

        assertThat(result).hasSize(2);
        ActivityMobilityRepresentativeRow first = result.get(0);
        assertThat(first)
                .extracting(
                        ActivityMobilityRepresentativeRow::startMonth,
                        ActivityMobilityRepresentativeRow::endMonth,
                        ActivityMobilityRepresentativeRow::arrivalDongCode,
                        ActivityMobilityRepresentativeRow::departureDongCode,
                        ActivityMobilityRepresentativeRow::observedMonthCount)
                .containsExactly("202601", "202603", "1113075", "1113075", 1);
        assertThat(first.totalMobility()).isCloseTo(90.0 / 64.0, within(0.000001));
        assertThat(first.avgTime()).isCloseTo(40.0, within(0.000001));

        ActivityMobilityRepresentativeRow second = result.get(1);
        assertThat(second)
                .extracting(
                        ActivityMobilityRepresentativeRow::startMonth,
                        ActivityMobilityRepresentativeRow::endMonth,
                        ActivityMobilityRepresentativeRow::arrivalDongCode,
                        ActivityMobilityRepresentativeRow::departureDongCode,
                        ActivityMobilityRepresentativeRow::observedMonthCount)
                .containsExactly("202601", "202603", "1113075", "1121058", 2);
        assertThat(second.totalMobility()).isCloseTo(396.0 / 64.0, within(0.000001));
        assertThat(second.avgTime()).isCloseTo(20.0, within(0.000001));
    }

    @Test
    @DisplayName("202601~202603 기간의 평일 수를 계산한다")
    void countsWeekdaysForAggregationPeriod() {
        assertThat(aggregator.countWeekdays("202601", "202603")).isEqualTo(64);
    }

    @Test
    @DisplayName("집계 기간 밖의 row가 있으면 실패한다")
    void rejectsRowsOutsideAggregationPeriod() {
        List<ActivityMobilityCsvRow> rows =
                List.of(row("202604", "1113075", "1121058", 198.0, 220.0, 110.0, 10.0));

        assertThatThrownBy(() -> aggregator.aggregate(rows, "202601", "202603"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("outside aggregation period")
                .hasMessageContaining("202604");
    }

    private ActivityMobilityCsvRow row(
            String month,
            String arrivalDongCode,
            String departureDongCode,
            double sourceTotalMobility,
            double commuteInPopulation,
            double commuteOutPopulation,
            double avgTime) {
        return new ActivityMobilityCsvRow(
                month,
                arrivalDongCode,
                departureDongCode,
                sourceTotalMobility,
                commuteInPopulation,
                commuteOutPopulation,
                avgTime);
    }
}
