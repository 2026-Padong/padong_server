package com.example.padong_server.domain.hotplace.util;

import com.example.padong_server.domain.hotplace.entity.Category;
import com.example.padong_server.domain.hotplace.entity.HotPlace;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class HotPlaceDataUtil {

    private static final String FILE_PATH = "data/store/realtimePlaceData.csv";
    private static final int EXPECTED_COLUMN_COUNT = 6;

    public List<HotPlace> readHotPlacesFromCsv() {
        long start = System.currentTimeMillis();

        try {
            List<HotPlace> result = readCsv(StandardCharsets.UTF_8);
            logElapsed(result.size(), start);
            return result;
        } catch (MalformedInputException e) {
            try {
                List<HotPlace> result = readCsv(Charset.forName("MS949"));
                logElapsed(result.size(), start);
                return result;
            } catch (IOException fallbackException) {
                throw new RuntimeException("Error reading realtime place CSV file", fallbackException);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error reading realtime place CSV file", e);
        }
    }

    private List<HotPlace> readCsv(Charset charset) throws IOException {
        List<HotPlace> result = new ArrayList<>();

        try (BufferedReader reader = openReader(charset)) {
            String line = reader.readLine();
            int lineNumber = 1;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank() || isEmptyRow(line)) {
                    continue;
                }

                List<String> columns = parseCsvLine(line);
                if (columns.size() != EXPECTED_COLUMN_COUNT) {
                    log.warn("HotPlace CSV line {} skipped. Expected {} columns but got {}", lineNumber, EXPECTED_COLUMN_COUNT, columns.size());
                    continue;
                }

                result.add(HotPlace.builder()
                        .category(Category.from(columns.get(0)))
                        .areaNm(columns.get(3))
                        .guName(columns.get(5))
                        .latitude(0.0)
                        .longitude(0.0)
                        .build());
            }
        }

        return result;
    }

    private BufferedReader openReader(Charset charset) throws IOException {
        ClassPathResource resource = new ClassPathResource(FILE_PATH);
        return new BufferedReader(new InputStreamReader(resource.getInputStream(), charset));
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

    private boolean isEmptyRow(String line) {
        return line.replace(",", "").replace("\"", "").isBlank();
    }

    private void logElapsed(int size, long start) {
        long end = System.currentTimeMillis();
        log.info("HotPlace: total {} rows, elapsed {}ms", size, end - start);
    }
}
