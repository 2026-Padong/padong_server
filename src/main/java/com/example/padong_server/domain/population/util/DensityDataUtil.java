package com.example.padong_server.domain.population.util;

import com.example.padong_server.domain.dongne.entity.AdminDong;
import com.example.padong_server.domain.dongne.repository.AdminDongRepository;
import com.example.padong_server.domain.dongne.service.DongneService;
import com.example.padong_server.domain.population.entity.PopulationDensity;
import com.opencsv.CSVReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DensityDataUtil {

    private static final String FILE_PATH = "data/population/seoul_admin_dong_population_density.csv";

    private final DongneService dongneService;
    private final AdminDongRepository adminDongRepository;

    public DensityDataUtil(DongneService dongneService, AdminDongRepository adminDongRepository) {
        this.dongneService = dongneService;
        this.adminDongRepository = adminDongRepository;
    }

    public List<PopulationDensity> readDensityFromCsv() {
        List<PopulationDensity> result = new ArrayList<>();

        ClassPathResource resource = new ClassPathResource(FILE_PATH);

        try (BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));
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
                            "Skip population density row because admin dong was not found. code={}, city={}, district={}, adminDong={}",
                            adminDongCode,
                            cityName,
                            districtName,
                            adminDongName
                    );
                    continue;
                }

                result.add(PopulationDensity.builder()
                        .adminDong(adminDong.get())
                        .cityName(cityName)
                        .districtName(districtName)
                        .adminDongName(adminDongName)
                        .totalPopulation(parse(fields[4]))
                        .areaSize(parse(fields[5]))
                        .density(parse(fields[6]))
                        .soccerFieldPopulation(parse(fields[7]))
                        .build());
            }
        } catch (IOException e) {
            log.error("Error reading density CSV file: {}", FILE_PATH, e);
            throw new RuntimeException("Error reading density CSV file", e);
        } catch (Exception e) {
            log.error("Error processing density CSV file: {}", FILE_PATH, e);
            throw new RuntimeException("Error processing density CSV file", e);
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
        String sanitized = sanitize(value);
        if (sanitized.isEmpty()) {
            return 0.0;
        }
        return Double.parseDouble(sanitized.replace(",", ""));
    }
}
