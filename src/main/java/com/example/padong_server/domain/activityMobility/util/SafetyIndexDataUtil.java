package com.example.padong_server.domain.activityMobility.util;

import com.example.padong_server.domain.activityMobility.dto.SafetyIndexCsvRow;
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

@Component
public class SafetyIndexDataUtil {

    private static final String FILE_PATH = "data/activity-mobility/seoul_safety_index.csv";

    public List<SafetyIndexCsvRow> readSafetyIndexRows() {
        try {
            return readCsv(StandardCharsets.UTF_8);
        } catch (MalformedInputException exception) {
            try {
                return readCsv(Charset.forName("MS949"));
            } catch (IOException fallbackException) {
                throw new IllegalStateException("Failed to read safety index csv", fallbackException);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read safety index csv", exception);
        }
    }

    private List<SafetyIndexCsvRow> readCsv(Charset charset) throws IOException {
        List<SafetyIndexCsvRow> rows = new ArrayList<>();

        ClassPathResource resource = new ClassPathResource(FILE_PATH);
        try (BufferedReader reader =
                     new BufferedReader(new InputStreamReader(resource.getInputStream(), charset))) {
            String line = reader.readLine();
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }

                String[] columns = line.split(",", -1);
                if (columns.length < 9) {
                    continue;
                }

                rows.add(new SafetyIndexCsvRow(
                        columns[1].trim(),
                        columns[2].trim(),
                        parseInt(columns[3]),
                        parseInt(columns[4]),
                        parseInt(columns[5]),
                        parseInt(columns[6]),
                        parseInt(columns[7]),
                        parseInt(columns[8])
                ));
            }
        }

        return rows;
    }

    private int parseInt(String value) {
        return Integer.parseInt(value.trim());
    }
}
