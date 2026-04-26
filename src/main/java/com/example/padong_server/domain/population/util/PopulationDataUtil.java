package com.example.padong_server.domain.population.util;

import com.example.padong_server.domain.dongne.entity.AdminDong;
import com.example.padong_server.domain.dongne.service.DongneService;
import com.example.padong_server.domain.population.entity.Population;
import com.opencsv.CSVReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class PopulationDataUtil {

    private final DongneService dongneService;

    public PopulationDataUtil(DongneService dongneService) {
        this.dongneService = dongneService;
    }

    private final String filePath = "data/population.csv";
    private final String dongneRegex = "^서울특별시\\s[가-힣]+구\\s[가-힣0-9\\.]+동\\(\\d{10}\\)$";

    public List<Population> readPopulationFromCsv() {
        List<Population> result = new ArrayList<>();
        ClassPathResource resource = new ClassPathResource(filePath);

        try (CSVReader reader = new CSVReader(
                new InputStreamReader(resource.getInputStream(), "EUC-KR"))) {

            String[] fields;
            boolean isFirstLine = true;

            while ((fields = reader.readNext()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }

                String rawRegion = fields[0].trim();
                if (!rawRegion.matches(dongneRegex)) continue;

                String dongneCode = rawRegion.substring(rawRegion.indexOf("(") + 1, rawRegion.indexOf(")")).trim();
                AdminDong adminDong = dongneService.findAdminDongByCode(dongneCode);

                // 전체 (계)
                result.add(Population.builder()
                        .adminDong(adminDong)
                        .totalPopulation(parse(fields[1]))
                        .build());
            }

        } catch (Exception e) {
            log.error("Error reading CSV file", e);
            throw new RuntimeException("Error reading CSV file", e);
        }

        System.out.println("최종 수집된 Population 수: " + result.size());
        return result;
    }

    private static double parse(String value) {
        if (value == null || value.isBlank())
            return 0.0;

        return Double.parseDouble(value.replace(",", "").replace("\"", "").trim());
    }
}
