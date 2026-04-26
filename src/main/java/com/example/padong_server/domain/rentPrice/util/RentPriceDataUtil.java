package com.example.padong_server.domain.rentPrice.util;

import com.example.padong_server.domain.rentPrice.dto.internal.RentPriceRawData;
import com.example.padong_server.domain.rentPrice.dto.internal.RentPriceRawData.RentRow;
import com.example.padong_server.domain.rentPrice.dto.internal.RentPriceRawData.RentType;
import com.example.padong_server.domain.rentPrice.dto.internal.RentPriceRawData.SaleRow;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class RentPriceDataUtil {

    private static final String RESOURCE_ROOT = "data/residence";

    static final List<RentPriceCsvFile> DEFAULT_FILES = List.of(
            saleFile("apartment", "아파트", "아파트(매매)_실거래가_20240418_ 20250417_with_법정동코드.csv", "전용면적(㎡)"),
            saleFile("apartment", "아파트", "아파트(매매)_실거래가_20250418_ 20260417_with_법정동코드.csv", "전용면적(㎡)"),
            rentFile("apartment", "아파트", "아파트(전월세)_실거래가_20240418_ 20250417_with_법정동코드.csv", "전용면적(㎡)"),
            rentFile("apartment", "아파트", "아파트(전월세)_실거래가_20250418_ 20260417_with_법정동코드.csv", "전용면적(㎡)"),
            saleFile("detached_multifamily", "단독다가구", "단독다가구(매매)_실거래가_20240418_ 20250417_with_법정동코드.csv", "연면적(㎡)"),
            saleFile("detached_multifamily", "단독다가구", "단독다가구(매매)_실거래가_20250418_ 20260417_with_법정동코드.csv", "연면적(㎡)"),
            rentFile("detached_multifamily", "단독다가구", "단독다가구(전월세)_실거래가_20240418_ 20250417_with_법정동코드.csv", "계약면적(㎡)"),
            rentFile("detached_multifamily", "단독다가구", "단독다가구(전월세)_실거래가_20250418_ 20260417_with_법정동코드.csv", "계약면적(㎡)"),
            saleFile("officetel", "오피스텔", "오피스텔(매매)_실거래가_20240418_ 20250417_with_법정동코드.csv", "전용면적(㎡)"),
            saleFile("officetel", "오피스텔", "오피스텔(매매)_실거래가_20250418_ 20260417_with_법정동코드.csv", "전용면적(㎡)"),
            rentFile("officetel", "오피스텔", "오피스텔(전월세)_실거래가_20240418_ 20250417_with_법정동코드.csv", "전용면적(㎡)"),
            rentFile("officetel", "오피스텔", "오피스텔(전월세)_실거래가_20250418_ 20260417_with_법정동코드.csv", "전용면적(㎡)"),
            saleFile("row_multifamily", "연립다세대", "연립다세대(매매)_실거래가_20240418_ 20250417_with_법정동코드.csv", "전용면적(㎡)"),
            saleFile("row_multifamily", "연립다세대", "연립다세대(매매)_실거래가_20250418_ 20260417_with_법정동코드.csv", "전용면적(㎡)"),
            rentFile("row_multifamily", "연립다세대", "연립다세대(전월세)_실거래가_20240418_ 20250417_with_법정동코드.csv", "전용면적(㎡)"),
            rentFile("row_multifamily", "연립다세대", "연립다세대(전월세)_실거래가_20250418_ 20260417_with_법정동코드.csv", "전용면적(㎡)")
    );

    public RentPriceRawData readRows() {
        RawDataBuilder builder = new RawDataBuilder();
        for (RentPriceCsvFile file : DEFAULT_FILES) {
            readClasspathFile(file, builder);
        }
        return builder.toRawData();
    }

    RentPriceRawData readRows(RentPriceCsvFile file, List<String> lines) {
        RawDataBuilder builder = new RawDataBuilder();
        readLines(file, lines.iterator(), builder);
        return builder.toRawData();
    }

    private void readClasspathFile(RentPriceCsvFile file, RawDataBuilder builder) {
        ClassPathResource resource = new ClassPathResource(file.resourcePath());
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            readReader(file, reader, builder);
        } catch (IOException exception) {
            throw new IllegalStateException("주거 실거래가 CSV를 읽을 수 없습니다: " + file.resourcePath(), exception);
        }
    }

    private void readReader(RentPriceCsvFile file, BufferedReader reader, RawDataBuilder builder) throws IOException {
        List<String> lines = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) {
            lines.add(line);
        }
        readLines(file, lines.iterator(), builder);
    }

    private void readLines(RentPriceCsvFile file, Iterator<String> lines, RawDataBuilder builder) {
        Map<String, Integer> headerIndex = null;
        int lineNumber = 0;
        while (lines.hasNext()) {
            lineNumber++;
            List<String> columns = parseCsvLine(lines.next());
            if (headerIndex == null) {
                if (!columns.isEmpty() && "NO".equals(normalize(columns.get(0)))) {
                    headerIndex = mapHeader(file, columns);
                }
                continue;
            }

            if (isBlankLine(columns)) {
                continue;
            }
            if (columns.size() != headerIndex.size()) {
                throw new IllegalArgumentException(
                        "CSV 컬럼 수가 헤더와 다릅니다: " + file.resourcePath() + ":" + lineNumber
                                + " expected=" + headerIndex.size() + " actual=" + columns.size()
                );
            }
            readDataRow(file, headerIndex, columns, builder);
        }

        if (headerIndex == null) {
            throw new IllegalArgumentException("CSV 헤더를 찾을 수 없습니다: " + file.resourcePath());
        }
    }

    private Map<String, Integer> mapHeader(RentPriceCsvFile file, List<String> header) {
        Map<String, Integer> headerIndex = new HashMap<>();
        for (int index = 0; index < header.size(); index++) {
            headerIndex.put(normalize(header.get(index)), index);
        }

        requireColumn(file, headerIndex, "법정동코드");
        requireColumn(file, headerIndex, file.areaColumn());
        if (file.sourceType() == SourceType.SALE) {
            requireColumn(file, headerIndex, "거래금액(만원)");
            requireColumn(file, headerIndex, "해제사유발생일");
        } else {
            requireColumn(file, headerIndex, "전월세구분");
            requireColumn(file, headerIndex, "보증금(만원)");
            requireColumn(file, headerIndex, "월세금(만원)");
        }
        return headerIndex;
    }

    private void requireColumn(RentPriceCsvFile file, Map<String, Integer> headerIndex, String columnName) {
        if (!headerIndex.containsKey(columnName)) {
            throw new IllegalArgumentException("CSV 필수 컬럼이 없습니다: " + file.resourcePath() + " column=" + columnName);
        }
    }

    private void readDataRow(
            RentPriceCsvFile file,
            Map<String, Integer> headerIndex,
            List<String> columns,
            RawDataBuilder builder
    ) {
        builder.sourceRowCount++;
        if (file.sourceType() == SourceType.SALE) {
            readSaleRow(file, headerIndex, columns, builder);
            return;
        }
        readRentRow(file, headerIndex, columns, builder);
    }

    private void readSaleRow(
            RentPriceCsvFile file,
            Map<String, Integer> headerIndex,
            List<String> columns,
            RawDataBuilder builder
    ) {
        String legalDongCode = get(columns, headerIndex, "법정동코드");
        String cancelReason = get(columns, headerIndex, "해제사유발생일");
        Optional<Long> salePrice = parseMoney(get(columns, headerIndex, "거래금액(만원)"));
        if (legalDongCode.isBlank() || salePrice.isEmpty() || hasCancelReason(cancelReason)) {
            builder.skippedRowCount++;
            return;
        }

        builder.saleRows.add(new SaleRow(
                legalDongCode,
                file.buildingType(),
                salePrice.get(),
                parsePositiveDecimal(get(columns, headerIndex, file.areaColumn())).orElse(null)
        ));
    }

    private void readRentRow(
            RentPriceCsvFile file,
            Map<String, Integer> headerIndex,
            List<String> columns,
            RawDataBuilder builder
    ) {
        String legalDongCode = get(columns, headerIndex, "법정동코드");
        RentType rentType = parseRentType(get(columns, headerIndex, "전월세구분")).orElse(null);
        Optional<Long> deposit = parseMoney(get(columns, headerIndex, "보증금(만원)"));
        Optional<Long> monthlyRent = parseMoney(get(columns, headerIndex, "월세금(만원)"));

        if (legalDongCode.isBlank() || rentType == null || deposit.isEmpty()) {
            builder.skippedRowCount++;
            return;
        }
        if (rentType == RentType.MONTHLY_RENT && monthlyRent.isEmpty()) {
            builder.skippedRowCount++;
            return;
        }

        builder.rentRows.add(new RentRow(
                legalDongCode,
                file.buildingType(),
                rentType,
                deposit.get(),
                rentType == RentType.MONTHLY_RENT ? monthlyRent.get() : null,
                parsePositiveDecimal(get(columns, headerIndex, file.areaColumn())).orElse(null)
        ));
    }

    static List<String> parseCsvLine(String line) {
        List<String> columns = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < line.length(); index++) {
            char character = line.charAt(index);
            if (character == '"') {
                if (quoted && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    current.append('"');
                    index++;
                    continue;
                }
                quoted = !quoted;
                continue;
            }
            if (character == ',' && !quoted) {
                columns.add(current.toString());
                current.setLength(0);
                continue;
            }
            current.append(character);
        }
        if (quoted) {
            throw new IllegalArgumentException("닫히지 않은 CSV 따옴표가 있습니다.");
        }
        columns.add(current.toString());
        return columns;
    }

    private static boolean isBlankLine(List<String> columns) {
        return columns.stream().allMatch(column -> normalize(column).isBlank());
    }

    private static String get(List<String> columns, Map<String, Integer> headerIndex, String columnName) {
        return normalize(columns.get(headerIndex.get(columnName)));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.replace("\uFEFF", "").trim();
    }

    private static boolean hasCancelReason(String cancelReason) {
        return !cancelReason.isBlank() && !"-".equals(cancelReason);
    }

    private static Optional<RentType> parseRentType(String rentType) {
        return switch (rentType) {
            case "전세" -> Optional.of(RentType.JEONSE);
            case "월세" -> Optional.of(RentType.MONTHLY_RENT);
            default -> Optional.empty();
        };
    }

    private static Optional<Long> parseMoney(String value) {
        String normalized = normalize(value).replace(",", "");
        if (normalized.isBlank() || "-".equals(normalized)) {
            return Optional.empty();
        }
        return Optional.of(Long.parseLong(normalized));
    }

    private static Optional<BigDecimal> parsePositiveDecimal(String value) {
        String normalized = normalize(value).replace(",", "");
        if (normalized.isBlank() || "-".equals(normalized)) {
            return Optional.empty();
        }
        BigDecimal decimal = new BigDecimal(normalized);
        if (decimal.compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.empty();
        }
        return Optional.of(decimal);
    }

    private static RentPriceCsvFile saleFile(String folderName, String buildingType, String fileName, String areaColumn) {
        return new RentPriceCsvFile(
                RESOURCE_ROOT + "/" + folderName + "/" + fileName,
                buildingType,
                SourceType.SALE,
                areaColumn
        );
    }

    private static RentPriceCsvFile rentFile(String folderName, String buildingType, String fileName, String areaColumn) {
        return new RentPriceCsvFile(
                RESOURCE_ROOT + "/" + folderName + "/" + fileName,
                buildingType,
                SourceType.RENT,
                areaColumn
        );
    }

    record RentPriceCsvFile(
            String resourcePath,
            String buildingType,
            SourceType sourceType,
            String areaColumn
    ) {
    }

    enum SourceType {
        SALE,
        RENT
    }

    private static class RawDataBuilder {
        private final List<SaleRow> saleRows = new ArrayList<>();
        private final List<RentRow> rentRows = new ArrayList<>();
        private long sourceRowCount;
        private long skippedRowCount;

        private RentPriceRawData toRawData() {
            return new RentPriceRawData(
                    List.copyOf(saleRows),
                    List.copyOf(rentRows),
                    sourceRowCount,
                    skippedRowCount
            );
        }
    }
}
