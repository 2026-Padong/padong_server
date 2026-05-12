package com.example.padong_server.domain.activityMobility.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.example.padong_server.domain.activityMobility.dto.ActivityMobilityCsvRow;
import com.example.padong_server.global.client.s3.S3CsvReaderService;
import com.example.padong_server.global.exception.CustomException;
import com.example.padong_server.global.exception.ErrorCode;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ActivityMobilityDataUtilTest {

    private final ActivityMobilityDataUtil activityMobilityDataUtil =
            new ActivityMobilityDataUtil(mock(S3CsvReaderService.class));

    @Test
    @DisplayName("Reads activity-mobility CSV rows and normalizes BOM, blanks, and numeric fields")
    void readsActivityMobilityCsvRows() {
        String csv =
                "\uFEFF기준월,직장행정동코드,거주행정동코드,출퇴근유동인구,출근인구,퇴근인구,평균이동시간\n"
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
    @DisplayName("Rejects unexpected CSV headers")
    void rejectsUnexpectedHeaders() {
        String csv =
                "기준월,직장행정동코드,거주행정동코드,출퇴근유동인구,출근인구,평균이동시간\n"
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
    @DisplayName("Rejects rows whose month does not match the expected file month")
    void rejectsUnexpectedMonth() {
        String csv =
                "기준월,직장행정동코드,거주행정동코드,출퇴근유동인구,출근인구,퇴근인구,평균이동시간\n"
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
    @DisplayName("Rejects blank admin-dong codes")
    void rejectsBlankAdminDongCodes() {
        String csv =
                "기준월,직장행정동코드,거주행정동코드,출퇴근유동인구,출근인구,퇴근인구,평균이동시간\n"
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
