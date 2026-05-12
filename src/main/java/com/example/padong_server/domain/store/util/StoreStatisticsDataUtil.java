package com.example.padong_server.domain.store.util;

import com.example.padong_server.domain.store.entity.StoreStatistics;
import com.example.padong_server.global.client.s3.S3CsvReaderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class StoreStatisticsDataUtil {

    private static final String S3_DOMAIN = "store";
    private static final String FILE_NAME = "storeData.csv";

    private final S3CsvReaderService s3CsvReaderService;

    public StoreStatisticsDataUtil(S3CsvReaderService s3CsvReaderService) {
        this.s3CsvReaderService = s3CsvReaderService;
    }

    public List<StoreStatistics> readStoreStatisticsFromCsv() {
        long start = System.currentTimeMillis();

        try {
            List<StoreStatistics> result = readCsv(StandardCharsets.UTF_8);
            logElapsed(result.size(), start);
            return result;
        } catch (MalformedInputException e) {
            try {
                List<StoreStatistics> result = readCsv(Charset.forName("MS949"));
                logElapsed(result.size(), start);
                return result;
            } catch (IOException fallbackException) {
                throw new RuntimeException("Error reading store statistics CSV file", fallbackException);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error reading store statistics CSV file", e);
        }
    }

    private List<StoreStatistics> readCsv(Charset charset) throws IOException {
        List<StoreStatistics> result = new ArrayList<>();

        try (BufferedReader reader = openReader(charset)) {
            String line = reader.readLine();
            int lineNumber = 1;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }

                List<String> columns = parseCsvLine(line);
                if (columns.size() != 8) {
                    log.warn("StoreStatistics CSV line {} skipped. Expected 8 columns but got {}", lineNumber, columns.size());
                    continue;
                }

                result.add(StoreStatistics.builder()
                        .baseYearQuarterCode(columns.get(0))
                        .adminDongCode(columns.get(1))
                        .adminDongName(columns.get(2))
                        .serviceCategoryCode(columns.get(3))
                        .serviceCategoryName(columns.get(4))
                        .storeCount(parseInteger(columns.get(5)))
                        .similarStoreCount(parseInteger(columns.get(6)))
                        .franchiseStoreCount(parseInteger(columns.get(7)))
                        .build());
            }
        }

        return result;
    }

    private BufferedReader openReader(Charset charset) throws IOException {
        InputStream inputStream = s3CsvReaderService.readFile(S3_DOMAIN, FILE_NAME);
        return new BufferedReader(new InputStreamReader(inputStream, charset));
    }

    private List<String> parseCsvLine(String line) {
        List<String> columns = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (ch == ',' && !inQuotes) {
                columns.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }

        columns.add(current.toString().trim());
        return columns;
    }

    private Integer parseInteger(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Integer.parseInt(value.replace(",", ""));
    }

    private void logElapsed(int size, long start) {
        long end = System.currentTimeMillis();
        log.info("StoreStatistics: total {} rows, elapsed {}ms", size, (end - start));
    }
}
