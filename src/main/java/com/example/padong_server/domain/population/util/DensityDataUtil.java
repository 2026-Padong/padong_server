package com.example.padong_server.domain.population.util;

import com.example.padongbe.domain.dongne.entity.AdminDong;
import com.example.padongbe.domain.dongne.service.DongneService;
import com.example.padongbe.domain.population.entity.Population;
import com.example.padongbe.domain.population.repository.PopulationRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

@Slf4j
@Component
public class DensityDataUtil {
    private final DongneService dongneService;

    private final PopulationRepository populationRepository;

    @Autowired
    public DensityDataUtil(DongneService dongneService, PopulationRepository populationRepository) {
        this.dongneService = dongneService;
        this.populationRepository = populationRepository;
    }

    private final String filePath = "data/면적데이터.xlsx";

    public void readDensityFromExcel() {

        ClassPathResource resource = new ClassPathResource(filePath);

        try (InputStream is = resource.getInputStream()) {
            Workbook workbook = new XSSFWorkbook(is);
            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String address = String.valueOf(row.getCell(0));
                double width = row.getCell(1).getNumericCellValue();

                AdminDong dong = dongneService.findAdminDongByAddress(address);

                Optional<Population> optional = populationRepository.findByAdminDong(dong);
                if (optional.isPresent()) {
                    Population population = optional.get();
                    population.setDensity(population.getTotalPopulation() / width);
                    populationRepository.save(population);
                }
            }
        } catch (IOException e) {
            log.error("Error reading Excel file");
            throw new RuntimeException("Error reading Excel file", e);
        } catch (Exception e) {
            log.error("Error processing Excel file");
            throw new RuntimeException("Error processing Excel file", e);
        }

    }

}
