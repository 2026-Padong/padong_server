package com.example.padong_server.domain.dongne.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.example.padong_server.domain.dongne.dto.AdminDongCsvRow;
import com.example.padong_server.domain.dongne.dto.DongMappingCsvRow;
import com.example.padong_server.domain.dongne.dto.LegalDongCsvRow;
import com.example.padong_server.global.client.s3.S3CsvReaderService;
import com.example.padong_server.global.exception.CustomException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class DongneDataUtilTest {

    private static final String DOMAIN = "dongne";

    @Mock
    private S3CsvReaderService s3CsvReaderService;

    private DongneDataUtil dongneDataUtil;

    @BeforeEach
    void setUp() {
        dongneDataUtil = new DongneDataUtil(s3CsvReaderService);
    }

    @Test
    void readsAdminDongRowsFromS3Csv() {
        when(s3CsvReaderService.readCsv(DOMAIN, "서울시_행정동_20260325.csv"))
                .thenReturn(
                        csvStream(
                                """
                                admin_dong_code,city_name,district_name,admin_dong_name,latitude,longitude,source_date,deleted_date
                                1111051500,서울특별시,종로구,청운효자동,37.5837762,126.9706629,20260325,
                                1111053000,서울특별시,종로구,사직동,37.5752046,126.9688076,20260325,20260326
                                """));

        List<AdminDongCsvRow> rows = dongneDataUtil.readAdminDongRows();

        assertEquals(1, rows.size());
        assertEquals("1111051500", rows.get(0).adminDongCode());
        assertEquals("서울특별시", rows.get(0).cityName());
        assertEquals("종로구", rows.get(0).districtName());
        assertEquals("청운효자동", rows.get(0).adminDongName());
        assertEquals(37.5837762, rows.get(0).latitude());
        assertEquals(126.9706629, rows.get(0).longitude());
        assertEquals("", rows.get(0).deletedDate());
    }

    @Test
    void readsLegalDongRowsFromS3Csv() {
        when(s3CsvReaderService.readCsv(DOMAIN, "서울시_법정동_20260325.csv"))
                .thenReturn(
                        csvStream(
                                """
                                legal_dong_code,city_name,district_name,legal_dong_name,legal_ri_name,source_date,deleted_date
                                1111010100,서울특별시,종로구,청운동,,20260325,
                                """));

        List<LegalDongCsvRow> rows = dongneDataUtil.readLegalDongRows();

        assertEquals(1, rows.size());
        assertEquals("1111010100", rows.get(0).legalDongCode());
        assertEquals("서울특별시", rows.get(0).cityName());
        assertEquals("종로구", rows.get(0).districtName());
        assertEquals("청운동", rows.get(0).legalDongName());
        assertEquals("", rows.get(0).deletedDate());
    }

    @Test
    void readsMappingRowsFromS3Csv() {
        when(s3CsvReaderService.readCsv(DOMAIN, "서울시_행정동_법정동_매핑_20260325.csv"))
                .thenReturn(
                        csvStream(
                                """
                                admin_dong_code,city_name,district_name,admin_dong_name,legal_dong_code,legal_dong_name,source_date,deleted_date
                                1111051500,서울특별시,종로구,청운효자동,1111010100,청운동,20260325,
                                """));

        List<DongMappingCsvRow> rows = dongneDataUtil.readDongMappingRows();

        assertEquals(1, rows.size());
        assertEquals("1111051500", rows.get(0).adminDongCode());
        assertEquals("1111010100", rows.get(0).legalDongCode());
        assertEquals("청운효자동", rows.get(0).adminDongName());
        assertEquals("청운동", rows.get(0).legalDongName());
        assertEquals("", rows.get(0).deletedDate());
    }

    @Test
    void throwsWhenHeadersDoNotMatchExpectedSchema() {
        when(s3CsvReaderService.readCsv(DOMAIN, "서울시_행정동_20260325.csv"))
                .thenReturn(csvStream("wrong,headers\nvalue1,value2\n"));

        assertThrows(CustomException.class, () -> dongneDataUtil.readAdminDongRows());
    }

    private InputStream csvStream(String value) {
        return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
    }
}
