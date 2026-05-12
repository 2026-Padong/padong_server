package com.example.padong_server.domain.activityMobility.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import com.example.padong_server.domain.activityMobility.dto.ActivityMobilityRepresentativeRow;
import com.example.padong_server.domain.activityMobility.entity.Mobility;
import com.example.padong_server.domain.dongne.dto.AdminDongCsvRow;
import com.example.padong_server.domain.dongne.entity.AdminDong;
import com.example.padong_server.domain.dongne.repository.AdminDongRepository;
import com.example.padong_server.global.client.s3.S3CsvReaderService;
import com.example.padong_server.global.exception.CustomException;
import com.example.padong_server.global.exception.ErrorCode;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ActivityMobilityEntityMapperTest {

    @Mock private AdminDongRepository adminDongRepository;
    @Mock private S3CsvReaderService s3CsvReaderService;

    private ActivityMobilityEntityMapper mapper;

    @BeforeEach
    void setUp() throws Exception {
        lenient().when(s3CsvReaderService.readFile("activity-mobility", "서울생활이동데이터_행정동코드_20210907.xlsx"))
                .thenAnswer(
                        ignored -> openCodebook());
        mapper = new ActivityMobilityEntityMapper(adminDongRepository, s3CsvReaderService);
    }

    @Test
    @DisplayName("Maps 7-digit mobility codes to AdminDong entities and returns Mobility rows")
    void mapsRepresentativeRowsToMobilityEntities() {
        AdminDong arrivalDong = adminDong("1111053000", "종로구", "사직동");
        AdminDong departureDong = adminDong("1162058500", "관악구", "성현동");
        when(adminDongRepository.findAll()).thenReturn(List.of(arrivalDong, departureDong));
        ActivityMobilityRepresentativeRow row =
                representativeRow("1101053", "1121058", 123.45, 35.6);

        List<Mobility> result = mapper.toEntities(List.of(row));

        assertThat(result).hasSize(1);
        Mobility mobility = result.get(0);
        assertThat(mobility)
                .extracting(
                        Mobility::getStartMonth,
                        Mobility::getEndMonth,
                        Mobility::getArrivalDong,
                        Mobility::getDepartureDong,
                        Mobility::getTotalMobility,
                        Mobility::getAvgTime)
                .containsExactly("202601", "202603", arrivalDong, departureDong, 123.45, 35.6);
        verify(adminDongRepository).findAll();
    }

    @Test
    @DisplayName("Applies legacy admin-type-code overrides")
    void appliesLegacyAdminTypeCodeReplacements() {
        AdminDong arrivalDong = adminDong("1150060300", "강서구", "가양제1동");
        AdminDong departureDong = adminDong("1168067500", "강남구", "개포3동");
        when(adminDongRepository.findAll()).thenReturn(List.of(arrivalDong, departureDong));
        ActivityMobilityRepresentativeRow row = representativeRow("1116064", "1123074", 12.3, 45.6);

        List<Mobility> result = mapper.toEntities(List.of(row));

        assertThat(result)
                .singleElement()
                .extracting(Mobility::getArrivalDong, Mobility::getDepartureDong)
                .containsExactly(arrivalDong, departureDong);
    }

    @Test
    @DisplayName("Maps 용신동 legacy code to 용두동 current AdminDong")
    void mapsYongsinDongToYongduDongFallback() {
        AdminDong arrivalDong = adminDong("1123053300", "동대문구", "용두동");
        when(adminDongRepository.findAll()).thenReturn(List.of(arrivalDong));
        ActivityMobilityRepresentativeRow row = representativeRow("1106081", "1106081", 12.3, 45.6);

        List<Mobility> result = mapper.toEntities(List.of(row));

        assertThat(result)
                .singleElement()
                .extracting(Mobility::getArrivalDong, Mobility::getDepartureDong)
                .containsExactly(arrivalDong, arrivalDong);
    }

    @Test
    @DisplayName("Maps codebook dong names without 제 to current AdminDong names with 제")
    void mapsNumberedDongNamesWithJePrefix() {
        AdminDong arrivalDong = adminDong("1111053000", "종로구", "사직동");
        AdminDong departureDong = adminDong("1111067000", "종로구", "창신제1동");
        when(adminDongRepository.findAll()).thenReturn(List.of(arrivalDong, departureDong));
        ActivityMobilityRepresentativeRow row =
                representativeRow("1101053", "1101067", 12.3, 45.6);

        List<Mobility> result = mapper.toEntities(List.of(row));

        assertThat(result)
                .singleElement()
                .extracting(Mobility::getArrivalDong, Mobility::getDepartureDong)
                .containsExactly(arrivalDong, departureDong);
    }

    @Test
    @DisplayName("Rejects unknown mobility admin-dong codes")
    void rejectsUnknownAdminDongCode() {
        when(adminDongRepository.findAll())
                .thenReturn(List.of(adminDong("1111053000", "종로구", "사직동")));
        ActivityMobilityRepresentativeRow row = representativeRow("9999999", "1101053", 12.3, 45.6);

        assertThatThrownBy(() -> mapper.toEntities(List.of(row)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    @DisplayName("Rejects when the current AdminDong is missing from DB")
    void rejectsMissingCurrentAdminDong() {
        when(adminDongRepository.findAll()).thenReturn(List.of());
        ActivityMobilityRepresentativeRow row = representativeRow("1101053", "1101053", 12.3, 45.6);

        assertThatThrownBy(() -> mapper.toEntities(List.of(row)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    @DisplayName("CSV mobility-dong codes must be 7-digit numbers")
    void rejectsInvalidCsvDongCodeFormat() {
        assertThatThrownBy(() -> mapper.normalizeMobilityDongCode("11130750"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    private InputStream openCodebook() throws Exception {
        String sharedStringsXml =
                """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" count="17" uniqueCount="17">
                  <si><t>시도</t></si>
                  <si><t>시군구</t></si>
                  <si><t>읍면동</t></si>
                  <si><t>name</t></si>
                  <si><t>full_name</t></si>
                  <si><t>1101053</t></si>
                  <si><t>사직동</t></si>
                  <si><t>서울특별시 종로구 사직동</t></si>
                  <si><t>1121058</t></si>
                  <si><t>성현동</t></si>
                  <si><t>서울특별시 관악구 성현동</t></si>
                  <si><t>9999999</t></si>
                  <si><t>알수없음</t></si>
                  <si><t>1116064</t></si>
                  <si><t>1123074</t></si>
                  <si><t>1101067</t></si>
                  <si><t>창신1동</t></si>
                </sst>
                """;
        String sheetXml =
                """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                  <sheetData>
                    <row r="1">
                      <c r="A1" t="s"><v>0</v></c><c r="B1" t="s"><v>1</v></c><c r="C1" t="s"><v>2</v></c><c r="D1" t="s"><v>3</v></c><c r="E1" t="s"><v>4</v></c>
                    </row>
                    <row r="2">
                      <c r="A2" t="inlineStr"><is><t>서울특별시</t></is></c><c r="B2" t="inlineStr"><is><t>종로구</t></is></c><c r="C2" t="s"><v>5</v></c><c r="D2" t="s"><v>6</v></c><c r="E2" t="s"><v>7</v></c>
                    </row>
                    <row r="3">
                      <c r="A3" t="inlineStr"><is><t>서울특별시</t></is></c><c r="B3" t="inlineStr"><is><t>관악구</t></is></c><c r="C3" t="s"><v>8</v></c><c r="D3" t="s"><v>9</v></c><c r="E3" t="s"><v>10</v></c>
                    </row>
                    <row r="4">
                      <c r="A4" t="inlineStr"><is><t>서울특별시</t></is></c><c r="B4" t="inlineStr"><is><t>미상구</t></is></c><c r="C4" t="s"><v>11</v></c><c r="D4" t="s"><v>12</v></c><c r="E4" t="inlineStr"><is><t>서울특별시 미상구 알수없음</t></is></c>
                    </row>
                    <row r="5">
                      <c r="A5" t="inlineStr"><is><t>서울특별시</t></is></c><c r="B5" t="inlineStr"><is><t>종로구</t></is></c><c r="C5" t="s"><v>15</v></c><c r="D5" t="s"><v>16</v></c><c r="E5" t="inlineStr"><is><t>서울특별시 종로구 창신1동</t></is></c>
                    </row>
                  </sheetData>
                </worksheet>
                """;

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream, StandardCharsets.UTF_8)) {
            zipOutputStream.putNextEntry(new ZipEntry("xl/sharedStrings.xml"));
            zipOutputStream.write(sharedStringsXml.getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();
            zipOutputStream.putNextEntry(new ZipEntry("xl/worksheets/sheet1.xml"));
            zipOutputStream.write(sheetXml.getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();
        }
        return new ByteArrayInputStream(outputStream.toByteArray());
    }

    private ActivityMobilityRepresentativeRow representativeRow(
            String arrivalDongCode,
            String departureDongCode,
            double totalMobility,
            double avgTime) {
        return new ActivityMobilityRepresentativeRow(
                "202601", "202603", arrivalDongCode, departureDongCode, totalMobility, avgTime, 3);
    }

    private AdminDong adminDong(String adminDongCode, String districtName, String adminDongName) {
        return new AdminDong(
                new AdminDongCsvRow(
                        adminDongCode,
                        "서울특별시",
                        districtName,
                        adminDongName,
                        37.5,
                        127.0,
                        "20081101",
                        ""));
    }
}
