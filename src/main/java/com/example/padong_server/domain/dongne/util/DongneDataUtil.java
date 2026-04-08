package com.example.padongbe.domain.dongne.util;

import com.example.padongbe.domain.dongne.dto.DongMappingDto;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
public class DongneDataUtil {

    private final String filePath = "data/dongne.xlsx";

    public List<DongMappingDto> readDongneFromExcel() {
        ClassPathResource file = new ClassPathResource(filePath);
        List<DongMappingDto> results = new ArrayList<>();

        try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String city = getString(row.getCell(0));
                String district = getString(row.getCell(1));
                String adminAreaName = getString(row.getCell(2));
                String adminDongName = getString(row.getCell(3));
                String legalDongName = getString(row.getCell(4));
                String adminTypeCode = getString(row.getCell(5));
                String adminDongCode = getString(row.getCell(6));
                String legalDongCode = getString(row.getCell(8));

                if (!city.equals("서울특별시")) continue;
                if(adminDongName.equals("서울특별시")) continue;
                if(adminDongName.endsWith("구")) continue;
                if (isDistrictOnly(adminDongName) && isDistrictOnly(legalDongName)) {
                    System.out.println(adminDongName + " " + legalDongName);
                    continue;
                }

                results.add(DongMappingDto.builder()
                        .city(city)
                        .district(district)
                        .adminAreaName(adminAreaName)
                        .adminDongName(adminDongName)
                        .legalDongName(legalDongName)
                        .adminTypeCode(adminTypeCode)
                        .adminDongCode(adminDongCode)
                        .legalDongCode(legalDongCode)
                        .build());
            }

        } catch (Exception e) {
            throw new RuntimeException("excel parsing error", e);
        }
        System.out.println(results.size());
        return results;
    }

    private static String getString(Cell cell) {
        if (cell == null) return "";
        cell.setCellType(CellType.STRING);
        return cell.getStringCellValue().trim();
    }

    private static boolean isDistrictOnly(String value) {
        return value.endsWith("구") && !value.contains("동");
    }
}
