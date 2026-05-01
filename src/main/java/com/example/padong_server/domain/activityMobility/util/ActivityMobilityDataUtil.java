package com.example.padong_server.domain.activityMobility.util;

import com.example.padong_server.domain.activityMobility.dto.ActivityMobilityCsvRow;
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
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ActivityMobilityDataUtil {

    private static final List<String> EXPECTED_CSV_HEADERS = List.of(
            "기준년월",
            "직장행정동코드",
            "거주행정동코드",
            "출퇴근가중치",
            "출근인구",
            "퇴근인구",
            "평균이동시간"
    );

    private static final List<ActivityMobilityCsvFile> CSV_FILES = List.of(
            new ActivityMobilityCsvFile("202601", "data/activity-mobility/202601_서울행정동간_생활이동.csv"),
            new ActivityMobilityCsvFile("202602", "data/activity-mobility/202602_서울행정동간_생활이동.csv"),
            new ActivityMobilityCsvFile("202603", "data/activity-mobility/202603_서울행정동간_생활이동.csv")
    );


    public List<ActivityMobilityCsvRow> readMonthlyCsvRows() {
        long start = System.currentTimeMillis();
        List<ActivityMobilityCsvRow> rows = new ArrayList<>();

        for (ActivityMobilityCsvFile csvFile : CSV_FILES) {
            ClassPathResource resource = new ClassPathResource(csvFile.path());
            try (InputStream inputStream = resource.getInputStream()) {
                rows.addAll(readCsvRows(csvFile.month(), inputStream, csvFile.path()));
            } catch (IOException exception) {
                throw new IllegalStateException("Failed to read activity mobility CSV: " + csvFile.path(), exception);
            }
        }

        long end = System.currentTimeMillis();
        log.info("ActivityMobility CSV: 총 {}건, 소요 시간: {}ms", rows.size(), (end - start));
        return rows;
    }

    List<ActivityMobilityCsvRow> readCsvRows(String expectedMonth, InputStream inputStream, String sourceName) {
        List<ActivityMobilityCsvRow> rows = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IllegalArgumentException("CSV header is missing: " + sourceName);
            }

            List<String> headers = parseCsvLine(headerLine).stream()
                    .map(ActivityMobilityDataUtil::normalize)
                    .toList();
            validateHeaders(sourceName, headers);

            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }

                List<String> values = parseCsvLine(line);
                Map<String, String> row = toRowMap(sourceName, lineNumber, headers, values);
                rows.add(toCsvRow(expectedMonth, sourceName, lineNumber, row));
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read activity mobility CSV: " + sourceName, exception);
        }

        return rows;
    }

    private ActivityMobilityCsvRow toCsvRow(
            String expectedMonth,
            String sourceName,
            int lineNumber,
            Map<String, String> row
    ) {
        String month = requireNonBlank(row, "기준년월", sourceName, lineNumber);
        if (!month.equals(expectedMonth)) {
            throw new IllegalArgumentException(
                    "Unexpected month in " + sourceName + " line " + lineNumber
                            + ". expected=" + expectedMonth + ", actual=" + month
            );
        }

        return new ActivityMobilityCsvRow(
                month,
                requireNonBlank(row, "직장행정동코드", sourceName, lineNumber),
                requireNonBlank(row, "거주행정동코드", sourceName, lineNumber),
                parseDouble(row.get("출퇴근가중치"), "출퇴근가중치", sourceName, lineNumber),
                parseDouble(row.get("출근인구"), "출근인구", sourceName, lineNumber),
                parseDouble(row.get("퇴근인구"), "퇴근인구", sourceName, lineNumber),
                parseDouble(row.get("평균이동시간"), "평균이동시간", sourceName, lineNumber)
        );
    }

    private void validateHeaders(String sourceName, List<String> actualHeaders) {
        if (!actualHeaders.equals(EXPECTED_CSV_HEADERS)) {
            throw new IllegalArgumentException(
                    "Unexpected CSV headers for " + sourceName
                            + ". expected=" + EXPECTED_CSV_HEADERS
                            + ", actual=" + actualHeaders
            );
        }
    }

    private Map<String, String> toRowMap(String sourceName, int lineNumber, List<String> headers, List<String> values) {
        if (values.size() != headers.size()) {
            throw new IllegalArgumentException(
                    "CSV column count mismatch in " + sourceName + " line " + lineNumber
                            + ". expected=" + headers.size() + ", actual=" + values.size()
            );
        }

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

    private String requireNonBlank(Map<String, String> row, String column, String sourceName, int lineNumber) {
        String value = normalize(row.get(column));
        if (value.isBlank()) {
            throw new IllegalArgumentException(
                    "Required CSV value is blank in " + sourceName + " line " + lineNumber + ": " + column
            );
        }
        return value;
    }

    private double parseDouble(String value, String column, String sourceName, int lineNumber) {
        String normalized = normalize(value).replace(",", "");
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(
                    "Required decimal is blank in " + sourceName + " line " + lineNumber + ": " + column
            );
        }
        try {
            return Double.parseDouble(normalized);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Invalid decimal in " + sourceName + " line " + lineNumber + " for " + column + ": " + value,
                    exception
            );
        }
    }

    private record ActivityMobilityCsvFile(String month, String path) {
    }
}
