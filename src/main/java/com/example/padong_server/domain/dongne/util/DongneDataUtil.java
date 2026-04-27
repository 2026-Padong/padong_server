package com.example.padong_server.domain.dongne.util;

import com.example.padong_server.domain.dongne.dto.AdminDongCsvRow;
import com.example.padong_server.domain.dongne.dto.DongMappingCsvRow;
import com.example.padong_server.domain.dongne.dto.LegalDongCsvRow;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Component
public class DongneDataUtil {

    private static final String ADMIN_DONG_PATH = "data/dongne/서울시_행정동_20260325.csv";
    private static final String LEGAL_DONG_PATH = "data/dongne/서울시_법정동_20260325.csv";
    private static final String MAPPING_PATH = "data/dongne/서울시_행정동_법정동_매핑_20260325.csv";

    public List<AdminDongCsvRow> readAdminDongRows() {
        return readCsv(
                ADMIN_DONG_PATH,
                List.of("admin_dong_code", "city_name", "district_name", "admin_dong_name", "address", "latitude", "longitude", "station_id", "distance_km"),
                values -> new AdminDongCsvRow(
                        values.get("admin_dong_code"),
                        values.get("city_name"),
                        values.get("district_name"),
                        values.get("admin_dong_name"),
                        parseDouble(values.get("latitude"), "latitude"),
                        parseDouble(values.get("longitude"), "longitude"),
                        parseLong(values.get("station_id"), "station_id")
                )
        );
    }

    public List<LegalDongCsvRow> readLegalDongRows() {
        return readCsv(
                LEGAL_DONG_PATH,
                List.of("legal_dong_code", "city_name", "district_name", "legal_dong_name", "legal_ri_name", "source_date", "deleted_date"),
                values -> new LegalDongCsvRow(
                        values.get("legal_dong_code"),
                        values.get("city_name"),
                        values.get("district_name"),
                        values.get("legal_dong_name"),
                        values.get("source_date"),
                        values.get("deleted_date")
                )
        );
    }

    public List<DongMappingCsvRow> readDongMappingRows() {
        return readCsv(
                MAPPING_PATH,
                List.of("admin_dong_code", "city_name", "district_name", "admin_dong_name", "legal_dong_code", "legal_dong_name", "source_date", "deleted_date"),
                values -> new DongMappingCsvRow(
                        values.get("admin_dong_code"),
                        values.get("city_name"),
                        values.get("district_name"),
                        values.get("admin_dong_name"),
                        values.get("legal_dong_code"),
                        values.get("legal_dong_name"),
                        values.get("source_date"),
                        values.get("deleted_date")
                )
        );
    }

    private <T> List<T> readCsv(String path, List<String> expectedHeaders, Function<Map<String, String>, T> mapper) {
        ClassPathResource resource = new ClassPathResource(path);
        List<T> rows = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IllegalArgumentException("CSV header is missing: " + path);
            }

            List<String> headers = parseCsvLine(headerLine).stream()
                    .map(this::normalize)
                    .toList();
            validateHeaders(path, headers, expectedHeaders);

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
            throw new IllegalStateException("Failed to read CSV: " + path, e);
        }

        return rows;
    }

    private void validateHeaders(String path, List<String> actualHeaders, List<String> expectedHeaders) {
        if (!actualHeaders.equals(expectedHeaders)) {
            throw new IllegalArgumentException(
                    "Unexpected CSV headers for " + path + ". expected=" + expectedHeaders + ", actual=" + actualHeaders
            );
        }
    }

    private Map<String, String> toRowMap(List<String> headers, List<String> values) {
        if (values.size() != headers.size()) {
            throw new IllegalArgumentException("CSV column count mismatch. expected=" + headers.size() + ", actual=" + values.size());
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

    private String normalize(String value) {
        return value.replace("\uFEFF", "").trim();
    }

    private Double parseDouble(String value, String label) {
        try {
            return Double.valueOf(normalize(value));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid decimal for " + label + ": " + value, exception);
        }
    }

    private Long parseLong(String value, String label) {
        try {
            return Long.valueOf(normalize(value));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid long for " + label + ": " + value, e);
        }
    }
}
