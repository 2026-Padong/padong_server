package com.example.padong_server.domain.dongne.util;

import com.example.padong_server.domain.dongne.dto.AdminDongCsvRow;
import com.example.padong_server.domain.dongne.dto.DongMappingCsvRow;
import com.example.padong_server.domain.dongne.dto.LegalDongCsvRow;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DongneDataUtilTest {

    private final DongneDataUtil dongneDataUtil = new DongneDataUtil();

    @Test
    void readsAdminDongRowsFromClasspathCsv() {
        List<AdminDongCsvRow> rows = dongneDataUtil.readAdminDongRows();

        assertEquals(427, rows.size());
        assertEquals("1111051500", rows.get(0).adminDongCode());
        assertEquals("서울특별시", rows.get(0).cityName());
        assertEquals("종로구", rows.get(0).districtName());
        assertEquals("청운효자동", rows.get(0).adminDongName());
        assertEquals(37.5837762, rows.get(0).latitude());
        assertEquals(126.9706629, rows.get(0).longitude());
        assertNotNull(rows.get(0).stationId());
        assertEquals(73L, rows.get(0).stationId());
    }

    @Test
    void readsLegalDongRowsFromClasspathCsv() {
        List<LegalDongCsvRow> rows = dongneDataUtil.readLegalDongRows();

        assertEquals(467, rows.size());
        assertEquals("1111010100", rows.get(0).legalDongCode());
        assertEquals("청운동", rows.get(0).legalDongName());
        assertTrueAllDeletedDatesBlank(rows.stream().map(LegalDongCsvRow::deletedDate).toList());
    }

    @Test
    void readsMappingRowsFromClasspathCsv() {
        List<DongMappingCsvRow> rows = dongneDataUtil.readDongMappingRows();

        assertEquals(746, rows.size());
        assertEquals("1111051500", rows.get(0).adminDongCode());
        assertEquals("1111010100", rows.get(0).legalDongCode());
        assertEquals("청운효자동", rows.get(0).adminDongName());
        assertEquals("청운동", rows.get(0).legalDongName());
        assertTrueAllDeletedDatesBlank(rows.stream().map(DongMappingCsvRow::deletedDate).toList());
    }

    private void assertTrueAllDeletedDatesBlank(List<String> deletedDates) {
        assertNotNull(deletedDates);
        deletedDates.forEach(value -> assertEquals("", value));
    }
}
