package com.example.padong_server.domain.activityMobility.util;

import com.example.padong_server.domain.activityMobility.dto.ActivityMobilityCsvRow;
import com.example.padong_server.global.client.s3.S3CsvReaderService;
import com.example.padong_server.global.exception.ErrorCode;
import com.example.padong_server.global.util.Preconditions;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ActivityMobilityDataUtil {

    private static final String S3_DOMAIN = "activity-mobility";
    private static final String CSV_READ_FAILURE_MESSAGE = "Failed to read activity mobility CSV";

    private static final List<String> EXPECTED_CSV_HEADERS =
            List.of("기준년월", "직장행정동코드", "거주행정동코드", "출퇴근가중치", "출근인구", "퇴근인구", "평균이동시간");

    private static final List<ActivityMobilityCsvFile> CSV_FILES =
            List.of(
                    new ActivityMobilityCsvFile("202601", "202601_서울행정동간_생활이동.csv"),
                    new ActivityMobilityCsvFile("202602", "202602_서울행정동간_생활이동.csv"),
                    new ActivityMobilityCsvFile("202603", "202603_서울행정동간_생활이동.csv"));

    private final S3CsvReaderService s3CsvReaderService;

    public ActivityMobilityDataUtil(S3CsvReaderService s3CsvReaderService) {
        this.s3CsvReaderService = s3CsvReaderService;
    }

    public List<ActivityMobilityCsvRow> readMonthlyCsvRows() {
        long start = System.currentTimeMillis();
        List<ActivityMobilityCsvRow> rows = new ArrayList<>();

        for (ActivityMobilityCsvFile csvFile : CSV_FILES) {
            long fileStart = System.currentTimeMillis();
            String sourceName = S3_DOMAIN + "/" + csvFile.filename();
            log.info("ActivityMobility CSV read started: source={}, month={}", sourceName, csvFile.month());
            try (InputStream inputStream = s3CsvReaderService.readFile(S3_DOMAIN, csvFile.filename())) {
                List<ActivityMobilityCsvRow> fileRows =
                        readCsvRows(csvFile.month(), inputStream, sourceName);
                rows.addAll(fileRows);
                log.info(
                        "ActivityMobility CSV read completed: source={}, month={}, rows={}, elapsedMs={}",
                        sourceName,
                        csvFile.month(),
                        fileRows.size(),
                        System.currentTimeMillis() - fileStart);
            } catch (IOException exception) {
                throw new IllegalStateException(
                        CSV_READ_FAILURE_MESSAGE + ": " + sourceName,
                        exception);
            } catch (RuntimeException exception) {
                log.error(
                        "ActivityMobility CSV read failed: source={}, month={}, elapsedMs={}, errorType={},"
                                + " message={}",
                        sourceName,
                        csvFile.month(),
                        System.currentTimeMillis() - fileStart,
                        exception.getClass().getName(),
                        exception.getMessage(),
                        exception);
                throw exception;
            }
        }

        long end = System.currentTimeMillis();
        log.info("ActivityMobility CSV: total {} rows, elapsed {}ms", rows.size(), end - start);
        return rows;
    }

    List<ActivityMobilityCsvRow> readCsvRows(
            String expectedMonth, InputStream inputStream, String sourceName) {
        List<ActivityMobilityCsvRow> rows = new ArrayList<>();

        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            Preconditions.validate(
                    headerLine != null,
                    ErrorCode.VALIDATION_ERROR,
                    "생활이동 CSV 헤더가 없습니다: source=" + sourceName);

            List<String> headers =
                    parseCsvLine(headerLine).stream().map(ActivityMobilityDataUtil::normalize).toList();
            log.info("ActivityMobility CSV headers parsed: source={}, headers={}", sourceName, headers);
            validateHeaders(headers);

            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }

                List<String> values = parseCsvLine(line);
                Map<String, String> row = toRowMap(headers, values);
                rows.add(toCsvRow(expectedMonth, sourceName, lineNumber, row));
            }
        } catch (IOException exception) {
            throw new IllegalStateException(CSV_READ_FAILURE_MESSAGE + ": " + sourceName, exception);
        }

        return rows;
    }

    private ActivityMobilityCsvRow toCsvRow(
            String expectedMonth, String sourceName, int lineNumber, Map<String, String> row) {
        String month = requireNonBlank(row, "기준년월");
        Preconditions.validate(
                month.equals(expectedMonth),
                ErrorCode.VALIDATION_ERROR,
                "생활이동 CSV 기준년월이 파일 월과 다릅니다: source="
                        + sourceName
                        + ", line="
                        + lineNumber
                        + ", expectedMonth="
                        + expectedMonth
                        + ", actualMonth="
                        + month);

        return new ActivityMobilityCsvRow(
                month,
                requireNonBlank(row, "직장행정동코드"),
                requireNonBlank(row, "거주행정동코드"),
                parseDouble(row.get("출퇴근가중치"), "출퇴근가중치", sourceName, lineNumber),
                parseDouble(row.get("출근인구"), "출근인구", sourceName, lineNumber),
                parseDouble(row.get("퇴근인구"), "퇴근인구", sourceName, lineNumber),
                parseDouble(row.get("평균이동시간"), "평균이동시간", sourceName, lineNumber));
    }

    private void validateHeaders(List<String> actualHeaders) {
        Preconditions.validate(
                actualHeaders.equals(EXPECTED_CSV_HEADERS),
                ErrorCode.VALIDATION_ERROR,
                "생활이동 CSV 헤더가 예상과 다릅니다: expected="
                        + EXPECTED_CSV_HEADERS
                        + ", actual="
                        + actualHeaders);
    }

    private Map<String, String> toRowMap(List<String> headers, List<String> values) {
        Preconditions.validate(
                values.size() == headers.size(),
                ErrorCode.VALIDATION_ERROR,
                "생활이동 CSV 컬럼 수가 헤더 수와 다릅니다: headers="
                        + headers.size()
                        + ", values="
                        + values.size());

        Map<String, String> row = new LinkedHashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            row.put(headers.get(i), normalize(values.get(i)));
        }
        return row;
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);

            if (ch == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                    continue;
                }
                inQuotes = !inQuotes;
                continue;
            }

            if (ch == ',' && !inQuotes) {
                values.add(current.toString());
                current.setLength(0);
                continue;
            }

            current.append(ch);
        }

        values.add(current.toString());
        return values;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.replace("\uFEFF", "").trim();
    }

    private String requireNonBlank(Map<String, String> row, String column) {
        String value = normalize(row.get(column));
        Preconditions.validate(
                !value.isBlank(),
                ErrorCode.VALIDATION_ERROR,
                "생활이동 CSV 필수 컬럼 값이 비어 있습니다: column=" + column);
        return value;
    }

    private double parseDouble(String value, String column, String sourceName, int lineNumber) {
        String normalized = normalize(value).replace(",", "");
        Preconditions.validate(
                !normalized.isBlank(),
                ErrorCode.VALIDATION_ERROR,
                "생활이동 CSV 숫자 컬럼 값이 비어 있습니다: source="
                        + sourceName
                        + ", line="
                        + lineNumber
                        + ", column="
                        + column);
        try {
            return Double.parseDouble(normalized);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Invalid decimal in "
                            + sourceName
                            + " line "
                            + lineNumber
                            + " for "
                            + column
                            + ": "
                            + value,
                    exception);
        }
    }

    private record ActivityMobilityCsvFile(String month, String filename) {}
}
