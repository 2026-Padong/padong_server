package com.example.padong_server.domain.activityMobility.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.padong_server.domain.activityMobility.dto.ActivityMobilityCsvRow;
import com.example.padong_server.global.exception.CustomException;
import com.example.padong_server.global.exception.ErrorCode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

class ActivityMobilityDataUtilTest {

    private final ActivityMobilityDataUtil activityMobilityDataUtil =
            new ActivityMobilityDataUtil();

    @Test
    @DisplayName("생활이동 월별 CSV row를 읽고 BOM, 공백, 숫자 값을 정규화한다")
    void readsActivityMobilityCsvRows() {
        String csv =
                "\uFEFF기준년월,직장행정동코드,거주행정동코드,출퇴근가중치,출근인구,퇴근인구,평균이동시간\n"
                        + "202603,1113075,1121058,41449.31,45053.56,27032.30,172.30\n"
                        + "\n"
                        + "202603,1119054,1113075,123.45,100,50,42\n";

        List<ActivityMobilityCsvRow> rows =
                activityMobilityDataUtil.readCsvRows("202603", inputStream(csv), "test.csv");

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0))
                .extracting(
                        ActivityMobilityCsvRow::month,
                        ActivityMobilityCsvRow::arrivalDongCode,
                        ActivityMobilityCsvRow::departureDongCode,
                        ActivityMobilityCsvRow::sourceTotalMobility,
                        ActivityMobilityCsvRow::commuteInPopulation,
                        ActivityMobilityCsvRow::commuteOutPopulation,
                        ActivityMobilityCsvRow::avgTime)
                .containsExactly(
                        "202603", "1113075", "1121058", 41449.31, 45053.56, 27032.30, 172.30);
    }

    @Test
    @DisplayName("CSV 헤더가 기대 스키마와 다르면 실패한다")
    void rejectsUnexpectedHeaders() {
        String csv =
                "기준년월,직장행정동코드,거주행정동코드,출퇴근가중치,출근인구,평균이동시간\n"
                        + "202603,1113075,1121058,41449.31,45053.56,172.30\n";

        assertThatThrownBy(
                        () ->
                                activityMobilityDataUtil.readCsvRows(
                                        "202603", inputStream(csv), "bad-header.csv"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    @DisplayName("파일 기준월과 row 기준년월이 다르면 실패한다")
    void rejectsUnexpectedMonth() {
        String csv =
                "기준년월,직장행정동코드,거주행정동코드,출퇴근가중치,출근인구,퇴근인구,평균이동시간\n"
                        + "202602,1113075,1121058,41449.31,45053.56,27032.30,172.30\n";

        assertThatThrownBy(
                        () ->
                                activityMobilityDataUtil.readCsvRows(
                                        "202603", inputStream(csv), "wrong-month.csv"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    @DisplayName("직장/거주 행정동 코드가 비어 있으면 실패한다")
    void rejectsBlankAdminDongCodes() {
        String csv =
                "기준년월,직장행정동코드,거주행정동코드,출퇴근가중치,출근인구,퇴근인구,평균이동시간\n"
                        + "202603,,1121058,41449.31,45053.56,27032.30,172.30\n";

        assertThatThrownBy(
                        () ->
                                activityMobilityDataUtil.readCsvRows(
                                        "202603", inputStream(csv), "blank-code.csv"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    private ByteArrayInputStream inputStream(String csv) {
        return new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
    }
}
