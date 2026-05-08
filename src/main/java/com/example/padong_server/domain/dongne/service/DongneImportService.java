package com.example.padong_server.domain.dongne.service;

import com.example.padong_server.domain.dongne.dto.AdminDongCsvRow;
import com.example.padong_server.domain.dongne.dto.DongMappingCsvRow;
import com.example.padong_server.domain.dongne.dto.DongneImportResult;
import com.example.padong_server.domain.dongne.dto.LegalDongCsvRow;
import com.example.padong_server.domain.dongne.entity.AdminDong;
import com.example.padong_server.domain.dongne.entity.DongMapping;
import com.example.padong_server.domain.dongne.entity.LegalDong;
import com.example.padong_server.domain.dongne.repository.AdminDongRepository;
import com.example.padong_server.domain.dongne.repository.DongMappingRepository;
import com.example.padong_server.domain.dongne.repository.LegalDongRepository;
import com.example.padong_server.domain.dongne.util.DongneDataUtil;
import com.example.padong_server.global.exception.ErrorCode;
import com.example.padong_server.global.util.Preconditions;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class DongneImportService {

    private static final String DUPLICATE_KEY_MESSAGE_FORMAT = "중복된 %s가 있습니다: %s";
    private static final String MAPPING_ADMIN_DONG_NOT_FOUND_MESSAGE_FORMAT =
            "매핑 대상 행정동 코드가 없습니다: %s";
    private static final String MAPPING_LEGAL_DONG_NOT_FOUND_MESSAGE_FORMAT =
            "매핑 대상 법정동 코드가 없습니다: %s";

    private final AdminDongRepository adminDongRepository;
    private final LegalDongRepository legalDongRepository;
    private final DongMappingRepository dongMappingRepository;
    private final DongneDataUtil dongneDataUtil;

    @Transactional
    public DongneImportResult importData() {
        List<AdminDongCsvRow> adminRows = dongneDataUtil.readAdminDongRows();
        List<LegalDongCsvRow> legalRows = dongneDataUtil.readLegalDongRows();
        List<DongMappingCsvRow> mappingRows = dongneDataUtil.readDongMappingRows();

        validateUnique(adminRows, AdminDongCsvRow::adminDongCode, "행정동 코드");
        validateUnique(legalRows, LegalDongCsvRow::legalDongCode, "법정동 코드");

        dongMappingRepository.deleteAllInBatch();
        legalDongRepository.deleteAllInBatch();
        adminDongRepository.deleteAllInBatch();

        List<AdminDong> savedAdmins =
                adminDongRepository.saveAll(adminRows.stream().map(AdminDong::new).toList());
        List<LegalDong> savedLegals =
                legalDongRepository.saveAll(legalRows.stream().map(LegalDong::new).toList());

        Map<String, AdminDong> adminByCode =
                indexBy(savedAdmins, AdminDong::getAdminDongCode, "행정동 코드");
        Map<String, LegalDong> legalByCode =
                indexBy(savedLegals, LegalDong::getLegalDongCode, "법정동 코드");

        List<DongMapping> mappings =
                mappingRows.stream()
                        .map(
                                row ->
                                        new DongMapping(
                                                requireAdmin(adminByCode, row.adminDongCode()),
                                                requireLegal(legalByCode, row.legalDongCode()),
                                                row))
                        .toList();

        List<DongMapping> savedMappings = dongMappingRepository.saveAll(mappings);

        return new DongneImportResult(savedAdmins.size(), savedLegals.size(), savedMappings.size());
    }

    private <T> void validateUnique(List<T> rows, Function<T, String> keyExtractor, String label) {
        indexBy(rows, keyExtractor, label);
    }

    private <T> Map<String, T> indexBy(
            List<T> rows, Function<T, String> keyExtractor, String label) {
        Map<String, T> result = new LinkedHashMap<>();
        for (T row : rows) {
            String key = keyExtractor.apply(row);
            T existing = result.putIfAbsent(key, row);
            Preconditions.validate(existing == null, ErrorCode.VALIDATION_ERROR);
        }
        return result;
    }

    private AdminDong requireAdmin(Map<String, AdminDong> adminByCode, String adminDongCode) {
        AdminDong adminDong = adminByCode.get(adminDongCode);
        Preconditions.validate(adminDong != null, ErrorCode.VALIDATION_ERROR);
        return adminDong;
    }

    private LegalDong requireLegal(Map<String, LegalDong> legalByCode, String legalDongCode) {
        LegalDong legalDong = legalByCode.get(legalDongCode);
        Preconditions.validate(legalDong != null, ErrorCode.VALIDATION_ERROR);
        return legalDong;
    }
}
