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
import com.example.padong_server.domain.subway.entity.Subway;
import com.example.padong_server.domain.subway.repository.SubwayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class DongneImportService {

    private final AdminDongRepository adminDongRepository;
    private final LegalDongRepository legalDongRepository;
    private final DongMappingRepository dongMappingRepository;
    private final DongneDataUtil dongneDataUtil;
    private final SubwayRepository subwayRepository;

    @Transactional
    public DongneImportResult importData() {
        List<AdminDongCsvRow> adminRows = dongneDataUtil.readAdminDongRows();
        List<LegalDongCsvRow> legalRows = dongneDataUtil.readLegalDongRows();
        List<DongMappingCsvRow> mappingRows = dongneDataUtil.readDongMappingRows();

        validateUnique(adminRows, AdminDongCsvRow::adminDongCode, "행정동 코드");
        validateUnique(legalRows, LegalDongCsvRow::legalDongCode, "법정동 코드");

        Map<Long, Subway> subwayById = indexBy(
                subwayRepository.findAllById(
                        adminRows.stream()
                                .map(AdminDongCsvRow::stationId)
                                .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll)
                ),
                Subway::getId,
                "지하철 역 ID"
        );

        dongMappingRepository.deleteAllInBatch();
        legalDongRepository.deleteAllInBatch();
        adminDongRepository.deleteAllInBatch();

        List<AdminDong> savedAdmins = adminDongRepository.saveAll(
                adminRows.stream()
                        .map(row -> new AdminDong(row, requireSubway(subwayById, row.stationId())))
                        .toList()
        );
        List<LegalDong> savedLegals = legalDongRepository.saveAll(
                legalRows.stream().map(LegalDong::new).toList()
        );

        Map<String, AdminDong> adminByCode = indexBy(savedAdmins, AdminDong::getAdminDongCode, "행정동 코드");
        Map<String, LegalDong> legalByCode = indexBy(savedLegals, LegalDong::getLegalDongCode, "법정동 코드");

        List<DongMapping> mappings = mappingRows.stream()
                .map(row -> new DongMapping(
                        requireAdmin(adminByCode, row.adminDongCode()),
                        requireLegal(legalByCode, row.legalDongCode()),
                        row
                ))
                .toList();

        List<DongMapping> savedMappings = dongMappingRepository.saveAll(mappings);

        return new DongneImportResult(savedAdmins.size(), savedLegals.size(), savedMappings.size());
    }

    private <T> void validateUnique(List<T> rows, Function<T, String> keyExtractor, String label) {
        indexBy(rows, keyExtractor, label);
    }

    private <K, T> Map<K, T> indexBy(List<T> rows, Function<T, K> keyExtractor, String label) {
        Map<K, T> result = new LinkedHashMap<>();
        for (T row : rows) {
            K key = keyExtractor.apply(row);
            T existing = result.putIfAbsent(key, row);
            if (existing != null) {
                throw new IllegalArgumentException("중복된 " + label + "가 있습니다: " + key);
            }
        }
        return result;
    }

    private AdminDong requireAdmin(Map<String, AdminDong> adminByCode, String adminDongCode) {
        AdminDong adminDong = adminByCode.get(adminDongCode);
        if (adminDong == null) {
            throw new IllegalArgumentException("매핑 대상 행정동 코드가 없습니다: " + adminDongCode);
        }
        return adminDong;
    }

    private LegalDong requireLegal(Map<String, LegalDong> legalByCode, String legalDongCode) {
        LegalDong legalDong = legalByCode.get(legalDongCode);
        if (legalDong == null) {
            throw new IllegalArgumentException("매핑 대상 법정동 코드가 없습니다: " + legalDongCode);
        }
        return legalDong;
    }

    private Subway requireSubway(Map<Long, Subway> subwayById, Long stationId) {
        Subway subway = subwayById.get(stationId);
        if (subway == null) {
            throw new IllegalArgumentException("행정동에 연결할 지하철 역 ID가 없습니다: " + stationId);
        }
        return subway;
    }
}
