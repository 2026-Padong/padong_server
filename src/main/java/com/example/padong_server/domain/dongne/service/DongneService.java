package com.example.padong_server.domain.dongne.service;

import com.example.padong_server.domain.dongne.dto.DongSuggestionItem;
import com.example.padong_server.domain.dongne.dto.DongSuggestionListResponse;
import com.example.padong_server.domain.dongne.dto.response.DistrictWithDongs;
import com.example.padong_server.domain.dongne.entity.AdminDong;
import com.example.padong_server.domain.dongne.entity.DongMapping;
import com.example.padong_server.domain.dongne.entity.LegalDong;
import com.example.padong_server.domain.dongne.repository.AdminDongRepository;
import com.example.padong_server.domain.dongne.repository.DongMappingRepository;
import com.example.padong_server.global.exception.CustomException;
import com.example.padong_server.global.exception.ErrorCode;
import com.example.padong_server.global.util.Preconditions;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DongneService {

    private static final String INVALID_ADDRESS_FORMAT_MESSAGE = "주소는 시/도 구/군 행정동 형식이어야 합니다.";
    private static final String ADMIN_DONG_ADDRESS_NOT_FOUND_MESSAGE = "존재하지 않는 행정동 주소입니다.";

    private final AdminDongRepository adminDongRepository;
    private final DongMappingRepository dongMappingRepository;
    private final DongneImportService dongneImportService;

    public void addDongneDate() {
        dongneImportService.importData();
    }

    public AdminDong findAdminDongByCode(String adminDongCode) {
        return adminDongRepository.getByAdminDongCode(adminDongCode);
    }

    public AdminDong findAdminDongByAddress(String address) {
        String[] addressParts = address.split(" ");
        Preconditions.validate(addressParts.length >= 3, ErrorCode.VALIDATION_ERROR);

        return adminDongRepository
                .findByCityNameAndDistrictNameAndAdminDongName(
                        addressParts[0], addressParts[1], addressParts[2])
                .orElseThrow(
                        () ->
                                new CustomException(
                                        ErrorCode.VALIDATION_ERROR,
                                        ADMIN_DONG_ADDRESS_NOT_FOUND_MESSAGE));
    }

    public List<DistrictWithDongs> getAdminDongTree() {
        Map<String, List<AdminDong>> grouped =
                adminDongRepository.findAll().stream()
                        .collect(Collectors.groupingBy(AdminDong::getDistrictName));

        return grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(
                        entry ->
                                new DistrictWithDongs(
                                        entry.getKey(),
                                        entry.getValue().stream()
                                                .sorted(
                                                        Comparator.comparing(
                                                                AdminDong::getAdminDongName))
                                                .map(
                                                        d ->
                                                                new DistrictWithDongs.AdminDongItem(
                                                                        d.getId(),
                                                                        d.getAdminDongName(),
                                                                        d.getAdminDongCode()))
                                                .toList()))
                .toList();
    }

    public LegalDong findLegalDongByAdminCode(String adminDongCode) {
        AdminDong adminDong = findAdminDongByCode(adminDongCode);
        List<DongMapping> mappings = dongMappingRepository.findByAdminDong(adminDong);
        if (mappings.isEmpty()) {
            return null;
        }
        return mappings.get(0).getLegalDong();
    }

    public DongSuggestionListResponse searchDongs(String q, int limit) {
        if (q == null || q.isBlank()) {
            return new DongSuggestionListResponse(List.of());
        }
        int cappedLimit = Math.min(Math.max(limit, 1), 50);
        List<DongSuggestionItem> items =
                adminDongRepository.searchByName(q.trim(), PageRequest.of(0, cappedLimit)).stream()
                        .map(DongSuggestionItem::from)
                        .toList();
        return new DongSuggestionListResponse(items);
    }

    /* legacy */
}
