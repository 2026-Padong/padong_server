package com.example.padong_server.domain.dongne.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import com.example.padong_server.domain.dongne.dto.AdminDongCsvRow;
import com.example.padong_server.domain.dongne.dto.DongMappingCsvRow;
import com.example.padong_server.domain.dongne.dto.DongneImportResult;
import com.example.padong_server.domain.dongne.dto.LegalDongCsvRow;
import com.example.padong_server.domain.dongne.repository.AdminDongRepository;
import com.example.padong_server.domain.dongne.repository.DongMappingRepository;
import com.example.padong_server.domain.dongne.repository.LegalDongRepository;
import com.example.padong_server.domain.dongne.util.DongneDataUtil;
import com.example.padong_server.global.exception.CustomException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class DongneImportServiceTest {

    @Mock private AdminDongRepository adminDongRepository;

    @Mock private LegalDongRepository legalDongRepository;

    @Mock private DongMappingRepository dongMappingRepository;

    @Mock private DongneDataUtil dongneDataUtil;

    @InjectMocks private DongneImportService dongneImportService;

    @Test
    void importsRowsAfterClearingExistingData() {
        AdminDongCsvRow adminRow =
                new AdminDongCsvRow(
                        "1111051500",
                        "서울특별시",
                        "종로구",
                        "청운효자동",
                        37.5837762,
                        126.9706629,
                        "20081101",
                        "");
        LegalDongCsvRow legalRow =
                new LegalDongCsvRow("1111010100", "서울특별시", "종로구", "청운동", "19880423", "");
        DongMappingCsvRow mappingRow =
                new DongMappingCsvRow(
                        "1111051500", "서울특별시", "종로구", "청운효자동", "1111010100", "청운동", "20081101", "");

        when(dongneDataUtil.readAdminDongRows()).thenReturn(List.of(adminRow));
        when(dongneDataUtil.readLegalDongRows()).thenReturn(List.of(legalRow));
        when(dongneDataUtil.readDongMappingRows()).thenReturn(List.of(mappingRow));
        when(adminDongRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(legalDongRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(dongMappingRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DongneImportResult result = dongneImportService.importData();

        assertEquals(1, result.adminDongCount());
        assertEquals(1, result.legalDongCount());
        assertEquals(1, result.mappingCount());

        InOrder inOrder = inOrder(dongMappingRepository, legalDongRepository, adminDongRepository);
        inOrder.verify(dongMappingRepository).deleteAllInBatch();
        inOrder.verify(legalDongRepository).deleteAllInBatch();
        inOrder.verify(adminDongRepository).deleteAllInBatch();
        inOrder.verify(adminDongRepository).saveAll(anyList());
        inOrder.verify(legalDongRepository).saveAll(anyList());
        inOrder.verify(dongMappingRepository).saveAll(anyList());
    }

    @Test
    void failsWhenMappingReferencesUnknownAdminCode() {
        AdminDongCsvRow adminRow =
                new AdminDongCsvRow(
                        "1111051500",
                        "서울특별시",
                        "종로구",
                        "청운효자동",
                        37.5837762,
                        126.9706629,
                        "20081101",
                        "");
        LegalDongCsvRow legalRow =
                new LegalDongCsvRow("1111010100", "서울특별시", "종로구", "청운동", "19880423", "");
        DongMappingCsvRow invalidMappingRow =
                new DongMappingCsvRow(
                        "9999999999", "서울특별시", "종로구", "없는동", "1111010100", "청운동", "20081101", "");

        when(dongneDataUtil.readAdminDongRows()).thenReturn(List.of(adminRow));
        when(dongneDataUtil.readLegalDongRows()).thenReturn(List.of(legalRow));
        when(dongneDataUtil.readDongMappingRows()).thenReturn(List.of(invalidMappingRow));
        when(adminDongRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(legalDongRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        try {
            dongneImportService.importData();
        } catch (CustomException exception) {
            assertEquals("매핑 대상 행정동 코드가 없습니다: 9999999999", exception.getMessage());
            return;
        }

        throw new AssertionError("Expected IllegalArgumentException to be thrown");
    }
}
