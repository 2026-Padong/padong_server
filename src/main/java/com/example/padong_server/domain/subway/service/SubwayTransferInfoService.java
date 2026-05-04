package com.example.padong_server.domain.subway.service;

import com.example.padong_server.domain.subway.entity.SubwayTransferInfo;
import com.example.padong_server.domain.subway.repository.SubwayTransferInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SubwayTransferInfoService {

    private final SubwayTransferInfoRepository subwayTransferInfoRepository;

    public List<String> findLinesByStationName(String stationName) {
        if (!StringUtils.hasText(stationName)) {
            return List.of();
        }

        List<SubwayTransferInfo> transferInfos = subwayTransferInfoRepository.findByStationNameIn(stationNameCandidates(stationName));
        if (transferInfos.isEmpty()) {
            return List.of();
        }

        Set<String> lines = new LinkedHashSet<>();
        for (SubwayTransferInfo transferInfo : transferInfos) {
            addNormalized(lines, transferInfo.getLine());
            addNormalized(lines, transferInfo.getTransferLine());
        }

        return List.copyOf(lines);
    }

    private List<String> stationNameCandidates(String stationName) {
        String trimmed = stationName.trim();
        String withoutSuffix = trimmed.endsWith("역") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
        String withSuffix = trimmed.endsWith("역") ? trimmed : trimmed + "역";
        return List.of(trimmed, withoutSuffix, withSuffix);
    }

    private void addNormalized(Set<String> lines, String line) {
        if (!StringUtils.hasText(line)) {
            return;
        }

        String normalized = line.trim();
        if (normalized.chars().allMatch(Character::isDigit)) {
            normalized = normalized + "호선";
        }

        lines.add(normalized);
    }
}
