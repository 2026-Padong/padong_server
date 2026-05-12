package com.example.padong_server.domain.population.util;

import com.example.padong_server.domain.dongne.entity.AdminDong;
import com.example.padong_server.domain.dongne.repository.AdminDongRepository;
import com.example.padong_server.domain.dongne.service.DongneService;
import com.example.padong_server.domain.population.entity.Population;
import com.example.padong_server.global.client.s3.S3CsvReaderService;
import com.opencsv.CSVReader;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PopulationDataUtil {

    private static final String S3_DOMAIN = "population";
    private static final String FILE_NAME = "seoul_admin_dong_population_density.csv";

    private final DongneService dongneService;
    private final AdminDongRepository adminDongRepository;
    private final S3CsvReaderService s3CsvReaderService;

    public PopulationDataUtil(
            DongneService dongneService,
            AdminDongRepository adminDongRepository,
            S3CsvReaderService s3CsvReaderService) {
        this.dongneService = dongneService;
        this.adminDongRepository = adminDongRepository;
        this.s3CsvReaderService = s3CsvReaderService;
    }

    public List<Population> readPopulationFromCsv() {
        List<Population> result = new ArrayList<>();

        try (InputStream inputStream = s3CsvReaderService.readFile(S3_DOMAIN, FILE_NAME);
             BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8));
             CSVReader reader = new CSVReader(bufferedReader)) {

            String[] fields;
            boolean isFirstLine = true;

            while ((fields = reader.readNext()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }

                String cityName = sanitize(fields[0]);
                String districtName = sanitize(fields[1]);
                String adminDongName = sanitize(fields[2]);
                String adminDongCode = sanitize(fields[3]);
                Optional<AdminDong> adminDong = resolveAdminDong(adminDongCode, cityName, districtName, adminDongName);

                if (adminDong.isEmpty()) {
                    log.warn(
                            "Skip population row because admin dong was not found. code={}, city={}, district={}, adminDong={}",
                            adminDongCode,
                            cityName,
                            districtName,
                            adminDongName
                    );
                    continue;
                }

                result.add(Population.builder()
                        .adminDong(adminDong.get())
                        .totalPopulation(parse(fields[4]))
                        .build());
            }

        } catch (Exception e) {
            log.error("Error reading CSV file", e);
            throw new RuntimeException("Error reading CSV file", e);
        }

        return result;
    }

    private Optional<AdminDong> resolveAdminDong(
            String adminDongCode,
            String cityName,
            String districtName,
            String adminDongName
    ) {
        Optional<AdminDong> byCode = adminDongRepository.findByAdminDongCode(adminDongCode);
        if (byCode.isPresent()) {
            return byCode;
        }
        return adminDongRepository.findByCityNameAndDistrictNameAndAdminDongName(
                cityName,
                districtName,
                adminDongName
        );
    }

    private static String sanitize(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\uFEFF", "").replace("\"", "").trim();
    }

    private static double parse(String value) {
        if (value == null || value.isBlank()) {
            return 0.0;
        }

        return Double.parseDouble(value.replace(",", "").replace("\"", "").trim());
    }
}
