package com.example.padong_server.domain.dongne.util;

import com.example.padong_server.domain.dongne.dto.AdminDongCsvRow;
import com.example.padong_server.domain.dongne.dto.DongMappingCsvRow;
import com.example.padong_server.domain.dongne.dto.LegalDongCsvRow;
import com.example.padong_server.global.exception.ErrorCode;
import com.example.padong_server.global.client.s3.S3CsvReaderService;
import com.example.padong_server.global.util.Preconditions;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Component
public class DongneDataUtil {

    private static final String DONGNE_DOMAIN = "dongne";
    private static final String ADMIN_DONG_FILENAME = "서울시_행정동_20260325.csv";
    private static final String LEGAL_DONG_FILENAME = "서울시_법정동_20260325.csv";
    private static final String MAPPING_FILENAME = "서울시_행정동_법정동_매핑_20260325.csv";
    private static final String CSV_HEADER_MISSING_MESSAGE_FORMAT = "CSV header is missing: %s";
    private static final String CSV_READ_FAILURE_MESSAGE_FORMAT = "Failed to read CSV: %s";
    private static final String UNEXPECTED_CSV_HEADERS_MESSAGE_FORMAT =
            "Unexpected CSV headers for %s. expected=%s, actual=%s";
    private static final String CSV_COLUMN_COUNT_MISMATCH_MESSAGE_FORMAT =
            "CSV column count mismatch. expected=%d, actual=%d";
    private static final String INVALID_DECIMAL_MESSAGE_FORMAT = "Invalid decimal for %s: %s";

    private final S3CsvReaderService s3CsvReaderService;

    public DongneDataUtil(S3CsvReaderService s3CsvReaderService) {
        this.s3CsvReaderService = s3CsvReaderService;
    }

    public List<AdminDongCsvRow> readAdminDongRows() {
        return readCsv(
                ADMIN_DONG_FILENAME,
                List.of(
                        "admin_dong_code",
                        "city_name",
                        "district_name",
                        "admin_dong_name",
                        "latitude",
                        "longitude",
                        "source_date",
                        "deleted_date"),
                values ->
                        new AdminDongCsvRow(
                                values.get("admin_dong_code"),
                                values.get("city_name"),
                                values.get("district_name"),
                                values.get("admin_dong_name"),
                                parseDouble(values.get("latitude"), "latitude"),
                                parseDouble(values.get("longitude"), "longitude"),
                                values.get("source_date"),
                                values.get("deleted_date")));
    }

    public List<LegalDongCsvRow> readLegalDongRows() {
        return readCsv(
                LEGAL_DONG_FILENAME,
                List.of(
                        "legal_dong_code",
                        "city_name",
                        "district_name",
                        "legal_dong_name",
                        "legal_ri_name",
                        "source_date",
                        "deleted_date"),
                values ->
                        new LegalDongCsvRow(
                                values.get("legal_dong_code"),
                                values.get("city_name"),
                                values.get("district_name"),
                                values.get("legal_dong_name"),
                                values.get("source_date"),
                                values.get("deleted_date")));
    }

    public List<DongMappingCsvRow> readDongMappingRows() {
        return readCsv(
                MAPPING_FILENAME,
                List.of(
                        "admin_dong_code",
                        "city_name",
                        "district_name",
                        "admin_dong_name",
                        "legal_dong_code",
                        "legal_dong_name",
                        "source_date",
                        "deleted_date"),
                values ->
                        new DongMappingCsvRow(
                                values.get("admin_dong_code"),
                                values.get("city_name"),
                                values.get("district_name"),
                                values.get("admin_dong_name"),
                                values.get("legal_dong_code"),
                                values.get("legal_dong_name"),
                                values.get("source_date"),
                                values.get("deleted_date")));
    }

    private <T> List<T> readCsv(
            String filename, List<String> expectedHeaders, Function<Map<String, String>, T> mapper) {
        List<T> rows = new ArrayList<>();

        try (InputStream inputStream = s3CsvReaderService.readCsv(DONGNE_DOMAIN, filename);
                BufferedReader reader =
                new BufferedReader(
                        new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            Preconditions.validate(headerLine != null, ErrorCode.VALIDATION_ERROR);

            List<String> headers = parseCsvLine(headerLine).stream().map(this::normalize).toList();
            validateHeaders(filename, headers, expectedHeaders);

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }

                List<String> values = parseCsvLine(line);
                Map<String, String> row = toRowMap(headers, values);
                if (!row.getOrDefault("deleted_date", "").isBlank()) {
                    continue;
                }
                rows.add(mapper.apply(row));
            }
        } catch (IOException e) {
            throw new IllegalStateException(
                    CSV_READ_FAILURE_MESSAGE_FORMAT.formatted(DONGNE_DOMAIN + "/" + filename), e);
        }

        return rows;
    }

    private void validateHeaders(
            String path, List<String> actualHeaders, List<String> expectedHeaders) {
        Preconditions.validate(actualHeaders.equals(expectedHeaders), ErrorCode.VALIDATION_ERROR);
    }

    private Map<String, String> toRowMap(List<String> headers, List<String> values) {
        Preconditions.validate(values.size() == headers.size(), ErrorCode.VALIDATION_ERROR);

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

    private String normalize(String value) {
        return value.replace("\uFEFF", "").trim();
    }

    private Double parseDouble(String value, String label) {
        try {
            return Double.valueOf(normalize(value));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    INVALID_DECIMAL_MESSAGE_FORMAT.formatted(label, value), exception);
        }
    }
}
