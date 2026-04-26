package com.example.padong_server.domain.activityMobility.util;

import com.example.padong_server.domain.activityMobility.entity.Mobility;
import com.example.padong_server.domain.dongne.entity.AdminDong;
import com.example.padong_server.domain.dongne.service.DongneService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class ActivityMobilityDataUtil {

    private final DongneService dongneService;

    public ActivityMobilityDataUtil(DongneService dongneService) {
        this.dongneService = dongneService;
    }

    private final String filePath = "data/HW_이동인구_요약_2025_03.xlsx";

    public List<Mobility> readActivityMobilityFromExcel() {
        long start = System.currentTimeMillis();
        List<Mobility> result = new ArrayList<>();
        ClassPathResource resource = new ClassPathResource(filePath);

        try (InputStream is = resource.getInputStream()) {
            Workbook workbook = new XSSFWorkbook(is);
            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String month = getString(row.getCell(0));
                String departureCode = getString(row.getCell(1));
                String arrivalCode = getString(row.getCell(2));
                double totalMobility = row.getCell(3).getNumericCellValue();
                double avgTime = row.getCell(4).getNumericCellValue();

                if (!departureCode.startsWith("11") || !arrivalCode.startsWith("11")) continue;
                AdminDong departureDong = dongneService.findAdminDongByTypeCode(departureCode+0);
                AdminDong arrivalDong = dongneService.findAdminDongByTypeCode(arrivalCode+0);


                Mobility mobility = Mobility.builder().month(month).arrivalDong(arrivalDong).departureDong(departureDong)
//                        .departureCode(departureCode)
//                        .arrivalCode(arrivalCode)
                        .totalMobility(totalMobility).avgTime(avgTime).build();

                result.add(mobility);
            }
        } catch (IOException e) {
            log.error("Error reading Excel file");
            throw new RuntimeException("Error reading Excel file", e);
        } catch (Exception e) {
            log.error("Error processing Excel file");
            throw new RuntimeException("Error processing Excel file", e);
        }

        long end = System.currentTimeMillis();
        log.info("RentPrice: 총 {}건, 소요 시간: {}ms", result.size(), (end - start));
        return result;
    }

    private static String getString(Cell cell) {
        if (cell == null) return "";
        if (cell.getCellType() == CellType.NUMERIC) {
            return String.valueOf((long) cell.getNumericCellValue()); // 소수점 제거
        }
        return cell.getStringCellValue().trim();
    }
}
