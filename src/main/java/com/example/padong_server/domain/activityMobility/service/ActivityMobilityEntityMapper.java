package com.example.padong_server.domain.activityMobility.service;

import com.example.padong_server.domain.activityMobility.dto.ActivityMobilityRepresentativeRow;
import com.example.padong_server.domain.activityMobility.entity.Mobility;
import com.example.padong_server.domain.dongne.entity.AdminDong;
import com.example.padong_server.domain.dongne.repository.AdminDongRepository;
import com.example.padong_server.global.exception.ErrorCode;
import com.example.padong_server.global.util.Preconditions;

import lombok.RequiredArgsConstructor;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

@Component
@RequiredArgsConstructor
public class ActivityMobilityEntityMapper {

    private static final int CSV_DONG_CODE_LENGTH = 7;
    private static final String SPREADSHEET_NS =
            "http://schemas.openxmlformats.org/spreadsheetml/2006/main";
    private static final String CODEBOOK_PATH =
            "data/activity-mobility/서울생활이동데이터_행정동코드_20210907.xlsx";
    private static final String SHARED_STRINGS_ENTRY = "xl/sharedStrings.xml";
    private static final String SHEET_ENTRY = "xl/worksheets/sheet1.xml";
    private static final String INVALID_ADMIN_DONG_CODE_MESSAGE_FORMAT =
            "Invalid AdminDong code: %s";
    private static final String MISSING_CODEBOOK_ADMIN_DONG_CODE_MESSAGE_FORMAT =
            "생활이동 코드북에 없는 행정동 코드입니다: %s=%s";
    private static final String MISSING_CURRENT_ADMIN_DONG_BY_NAME_MESSAGE_FORMAT =
            "생활이동 행정동명에 대응하는 현재 AdminDong이 없습니다: %s=%s, fullName=%s";
    private static final String MISSING_OVERRIDE_ADMIN_DONG_MESSAGE_FORMAT =
            "생활이동 행정동 보정 대상 AdminDong이 없습니다: %s=%s, adminDongCode=%s";
    private static final String CODEBOOK_READ_FAILURE_MESSAGE_FORMAT =
            "생활이동 행정동 코드북을 읽을 수 없습니다: %s";
    private static final String INVALID_CODEBOOK_XLSX_STRUCTURE_MESSAGE_FORMAT =
            "생활이동 행정동 코드북 xlsx 구조가 예상과 다릅니다: %s";
    private static final String INVALID_SHARED_STRING_INDEX_MESSAGE_FORMAT =
            "생활이동 코드북 shared string index가 잘못되었습니다: %s";
    private static final String CODEBOOK_XML_PARSE_FAILURE_MESSAGE =
            "생활이동 행정동 코드북 XML을 파싱할 수 없습니다.";
    private static final String EMPTY_CODEBOOK_MESSAGE_FORMAT = "생활이동 행정동 코드북이 비어 있습니다: %s";
    private static final String INVALID_CODEBOOK_HEADERS_MESSAGE_FORMAT =
            "생활이동 행정동 코드북 헤더가 다릅니다. expected=%s, actual=%s";
    private static final String DUPLICATE_CODEBOOK_CODE_MESSAGE_FORMAT =
            "생활이동 행정동 코드북 코드가 중복됩니다: %s";
    private static final String CSV_ADMIN_DONG_CODE_REQUIRED_MESSAGE = "CSV 행정동 코드가 비어 있습니다.";
    private static final String INVALID_CSV_ADMIN_DONG_CODE_MESSAGE_FORMAT =
            "CSV 행정동 코드는 7자리 숫자여야 합니다: %s";
    private static final String INVALID_CODEBOOK_ADDRESS_MESSAGE_FORMAT =
            "생활이동 코드북 주소 형식이 잘못되었습니다: %s=%s, fullName=%s";
    private static final List<String> EXPECTED_CODEBOOK_HEADERS =
            List.of("시도", "시군구", "읍면동", "name", "full_name");
    private static final Map<String, String> CURRENT_ADMIN_DONG_CODE_OVERRIDES =
            Map.of(
                    "11160640", "1150060300",
                    "11230740", "1168067500",
                    "11160720", "1150064100",
                    "11170680", "1153078000",
                    "11250510", "1174051500",
                    "11250520", "1174052500",
                    "11060810", "1123053300");

    private final AdminDongRepository adminDongRepository;

    public List<Mobility> toEntities(List<ActivityMobilityRepresentativeRow> rows) {
        Map<String, ActivityMobilityCodebookRow> codebookByMobilityCode =
                loadCodebookByMobilityCode();
        AdminDongIndex adminDongIndex = loadAdminDongIndex();
        return rows.stream()
                .map(row -> toEntity(row, codebookByMobilityCode, adminDongIndex))
                .toList();
    }

    private AdminDongIndex loadAdminDongIndex() {
        Map<String, AdminDong> adminDongByCode = new LinkedHashMap<>();
        Map<AdminDongNameKey, AdminDong> adminDongByDistrictAndName = new LinkedHashMap<>();
        for (AdminDong adminDong : adminDongRepository.findAll()) {
            String adminDongCode = adminDong.getAdminDongCode();
            Preconditions.validate(
                    adminDongCode != null && !adminDongCode.isBlank(), ErrorCode.VALIDATION_ERROR);
            adminDongByCode.put(adminDongCode, adminDong);
            adminDongByDistrictAndName.put(
                    new AdminDongNameKey(
                            adminDong.getDistrictName(),
                            normalizeDongName(adminDong.getAdminDongName())),
                    adminDong);
        }
        return new AdminDongIndex(adminDongByCode, adminDongByDistrictAndName);
    }

    private Mobility toEntity(
            ActivityMobilityRepresentativeRow row,
            Map<String, ActivityMobilityCodebookRow> codebookByMobilityCode,
            AdminDongIndex adminDongIndex) {
        AdminDong arrivalDong =
                requireAdminDong(
                        codebookByMobilityCode,
                        adminDongIndex,
                        row.arrivalDongCode(),
                        "arrivalDongCode");
        AdminDong departureDong =
                requireAdminDong(
                        codebookByMobilityCode,
                        adminDongIndex,
                        row.departureDongCode(),
                        "departureDongCode");

        return Mobility.builder()
                .startMonth(row.startMonth())
                .endMonth(row.endMonth())
                .arrivalDong(arrivalDong)
                .departureDong(departureDong)
                .totalMobility(row.totalMobility())
                .avgTime(row.avgTime())
                .build();
    }

    private AdminDong requireAdminDong(
            Map<String, ActivityMobilityCodebookRow> codebookByMobilityCode,
            AdminDongIndex adminDongIndex,
            String mobilityDongCode,
            String fieldName) {
        String normalizedMobilityDongCode = normalizeMobilityDongCode(mobilityDongCode);
        String adminTypeCode = normalizedMobilityDongCode + "0";
        String overrideAdminDongCode = CURRENT_ADMIN_DONG_CODE_OVERRIDES.get(adminTypeCode);
        if (overrideAdminDongCode != null) {
            return requireAdminDongByCode(
                    adminDongIndex, overrideAdminDongCode, normalizedMobilityDongCode, fieldName);
        }

        ActivityMobilityCodebookRow codebookRow =
                codebookByMobilityCode.get(normalizedMobilityDongCode);
        Preconditions.validate(codebookRow != null, ErrorCode.VALIDATION_ERROR);

        String districtName =
                extractDistrictName(codebookRow.fullName(), normalizedMobilityDongCode, fieldName);
        AdminDongNameKey key =
                new AdminDongNameKey(districtName, normalizeDongName(codebookRow.name()));
        AdminDong adminDong = adminDongIndex.byDistrictAndName().get(key);
        Preconditions.validate(adminDong != null, ErrorCode.VALIDATION_ERROR);
        return adminDong;
    }

    private AdminDong requireAdminDongByCode(
            AdminDongIndex adminDongIndex,
            String adminDongCode,
            String mobilityDongCode,
            String fieldName) {
        AdminDong adminDong = adminDongIndex.byCode().get(adminDongCode);
        Preconditions.validate(adminDong != null, ErrorCode.VALIDATION_ERROR);
        return adminDong;
    }

    private Map<String, ActivityMobilityCodebookRow> loadCodebookByMobilityCode() {
        ClassPathResource resource = new ClassPathResource(CODEBOOK_PATH);
        try (InputStream inputStream = resource.getInputStream()) {
            Map<String, byte[]> entries = readXlsxEntries(inputStream);
            List<String> sharedStrings = readSharedStrings(entries);
            List<List<String>> sheetRows = readSheetRows(entries, sharedStrings);
            return toCodebookByMobilityCode(sheetRows);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    CODEBOOK_READ_FAILURE_MESSAGE_FORMAT.formatted(CODEBOOK_PATH), exception);
        }
    }

    private Map<String, byte[]> readXlsxEntries(InputStream inputStream) throws IOException {
        Map<String, byte[]> entries = new HashMap<>();
        try (ZipInputStream zipInputStream =
                new ZipInputStream(inputStream, StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (SHARED_STRINGS_ENTRY.equals(entry.getName())
                        || SHEET_ENTRY.equals(entry.getName())) {
                    entries.put(entry.getName(), zipInputStream.readAllBytes());
                }
            }
        }
        Preconditions.validate(
                entries.containsKey(SHARED_STRINGS_ENTRY) && entries.containsKey(SHEET_ENTRY),
                ErrorCode.VALIDATION_ERROR);
        return entries;
    }

    private List<String> readSharedStrings(Map<String, byte[]> entries) {
        Document document = parseXml(entries.get(SHARED_STRINGS_ENTRY));
        NodeList stringItems = document.getElementsByTagNameNS(SPREADSHEET_NS, "si");
        List<String> sharedStrings = new ArrayList<>();
        for (int i = 0; i < stringItems.getLength(); i++) {
            Element stringItem = (Element) stringItems.item(i);
            NodeList textItems = stringItem.getElementsByTagNameNS(SPREADSHEET_NS, "t");
            StringBuilder value = new StringBuilder();
            for (int j = 0; j < textItems.getLength(); j++) {
                value.append(textItems.item(j).getTextContent());
            }
            sharedStrings.add(normalize(value.toString()));
        }
        return sharedStrings;
    }

    private List<List<String>> readSheetRows(
            Map<String, byte[]> entries, List<String> sharedStrings) {
        Document document = parseXml(entries.get(SHEET_ENTRY));
        NodeList rowItems = document.getElementsByTagNameNS(SPREADSHEET_NS, "row");
        List<List<String>> rows = new ArrayList<>();
        for (int i = 0; i < rowItems.getLength(); i++) {
            Element rowItem = (Element) rowItems.item(i);
            NodeList cellItems = rowItem.getElementsByTagNameNS(SPREADSHEET_NS, "c");
            List<String> rowValues = new ArrayList<>();
            for (int j = 0; j < cellItems.getLength(); j++) {
                Element cellItem = (Element) cellItems.item(j);
                int columnIndex = resolveColumnIndex(cellItem, rowValues.size());
                while (rowValues.size() <= columnIndex) {
                    rowValues.add("");
                }
                rowValues.set(columnIndex, readCellValue(cellItem, sharedStrings));
            }
            rows.add(rowValues);
        }
        return rows;
    }

    private int resolveColumnIndex(Element cellItem, int fallbackColumnIndex) {
        String reference = cellItem.getAttribute("r");
        if (reference == null || reference.isBlank()) {
            return fallbackColumnIndex;
        }

        int columnIndex = 0;
        int digitStartIndex = 0;
        while (digitStartIndex < reference.length()
                && Character.isAlphabetic(reference.charAt(digitStartIndex))) {
            char column = Character.toUpperCase(reference.charAt(digitStartIndex));
            columnIndex = columnIndex * 26 + column - 'A' + 1;
            digitStartIndex++;
        }
        return columnIndex == 0 ? fallbackColumnIndex : columnIndex - 1;
    }

    private String readCellValue(Element cellItem, List<String> sharedStrings) {
        String type = cellItem.getAttribute("t");
        if ("inlineStr".equals(type)) {
            NodeList textItems = cellItem.getElementsByTagNameNS(SPREADSHEET_NS, "t");
            return textItems.getLength() == 0 ? "" : normalize(textItems.item(0).getTextContent());
        }

        NodeList valueItems = cellItem.getElementsByTagNameNS(SPREADSHEET_NS, "v");
        if (valueItems.getLength() == 0) {
            return "";
        }

        String rawValue = normalize(valueItems.item(0).getTextContent());
        if ("s".equals(type)) {
            int sharedStringIndex = Integer.parseInt(rawValue);
            Preconditions.validate(
                    sharedStringIndex >= 0 && sharedStringIndex < sharedStrings.size(),
                    ErrorCode.VALIDATION_ERROR);
            return sharedStrings.get(sharedStringIndex);
        }
        return rawValue;
    }

    private Document parseXml(byte[] xmlBytes) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            return factory.newDocumentBuilder().parse(new ByteArrayInputStream(xmlBytes));
        } catch (IOException | ParserConfigurationException | SAXException exception) {
            throw new IllegalStateException(CODEBOOK_XML_PARSE_FAILURE_MESSAGE, exception);
        }
    }

    private Map<String, ActivityMobilityCodebookRow> toCodebookByMobilityCode(
            List<List<String>> sheetRows) {
        Preconditions.validate(!sheetRows.isEmpty(), ErrorCode.VALIDATION_ERROR);

        List<String> headers =
                sheetRows.get(0).stream().map(ActivityMobilityEntityMapper::normalize).toList();
        Preconditions.validate(
                headers.equals(EXPECTED_CODEBOOK_HEADERS), ErrorCode.VALIDATION_ERROR);

        Map<String, ActivityMobilityCodebookRow> codebookByMobilityCode = new LinkedHashMap<>();
        for (int i = 1; i < sheetRows.size(); i++) {
            List<String> row = sheetRows.get(i);
            if (row.size() < EXPECTED_CODEBOOK_HEADERS.size()) {
                continue;
            }

            String mobilityDongCode = normalize(row.get(2));
            if (mobilityDongCode.length() != CSV_DONG_CODE_LENGTH) {
                continue;
            }

            ActivityMobilityCodebookRow previous =
                    codebookByMobilityCode.putIfAbsent(
                            normalizeMobilityDongCode(mobilityDongCode),
                            new ActivityMobilityCodebookRow(
                                    mobilityDongCode,
                                    normalize(row.get(3)),
                                    normalize(row.get(4))));
            Preconditions.validate(previous == null, ErrorCode.VALIDATION_ERROR);
        }
        return codebookByMobilityCode;
    }

    String normalizeMobilityDongCode(String mobilityDongCode) {
        Preconditions.validate(mobilityDongCode != null, ErrorCode.VALIDATION_ERROR);
        String normalized = normalize(mobilityDongCode);
        Preconditions.validate(
                normalized.length() == CSV_DONG_CODE_LENGTH
                        && normalized.chars().allMatch(Character::isDigit),
                ErrorCode.VALIDATION_ERROR);
        return normalized;
    }

    private String extractDistrictName(String fullName, String mobilityDongCode, String fieldName) {
        String[] parts = normalize(fullName).split(" ");
        Preconditions.validate(
                parts.length >= 3 && "서울특별시".equals(parts[0]), ErrorCode.VALIDATION_ERROR);
        return parts[1];
    }

    private static String normalize(String value) {
        return value == null ? "" : value.replace("\uFEFF", "").trim();
    }

    private static String normalizeDongName(String value) {
        return normalize(value).replace("제", "").replace(".", "").replace("·", "").replace(" ", "");
    }

    private record ActivityMobilityCodebookRow(
            String mobilityDongCode, String name, String fullName) {}

    private record AdminDongIndex(
            Map<String, AdminDong> byCode, Map<AdminDongNameKey, AdminDong> byDistrictAndName) {}

    private record AdminDongNameKey(String districtName, String normalizedAdminDongName) {}
}
