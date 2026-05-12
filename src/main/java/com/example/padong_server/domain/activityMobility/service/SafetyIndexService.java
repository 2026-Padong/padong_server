package com.example.padong_server.domain.activityMobility.service;

import com.example.padong_server.domain.activityMobility.dto.SafetyIndexCsvRow;
import com.example.padong_server.domain.activityMobility.dto.SafetyIndexResponse;
import com.example.padong_server.domain.activityMobility.entity.SafetyIndex;
import com.example.padong_server.domain.activityMobility.repository.SafetyIndexRepository;
import com.example.padong_server.domain.activityMobility.util.SafetyIndexDataUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SafetyIndexService {

    private final SafetyIndexRepository safetyIndexRepository;
    private final SafetyIndexDataUtil safetyIndexDataUtil;

    @Transactional
    public String importData() {
        List<SafetyIndexCsvRow> rows = safetyIndexDataUtil.readSafetyIndexRows();
        safetyIndexRepository.deleteAllInBatch();
        safetyIndexRepository.saveAll(rows.stream().map(this::toEntity).toList());
        return "안전지수 데이터 적재 완료: " + rows.size() + "건";
    }

    @Transactional(readOnly = true)
    public Optional<SafetyIndexResponse> findResponse(String cityName, String districtName) {
        return findSafetyIndex(cityName, districtName)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public String getOverallGrade(String cityName, String districtName) {
        return findSafetyIndex(cityName, districtName)
                .map(this::overallGrade)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public double getAverageScore(String cityName, String districtName) {
        return findSafetyIndex(cityName, districtName)
                .map(this::usedAverageScore)
                .orElse(0.0);
    }

    private Optional<SafetyIndex> findSafetyIndex(String cityName, String districtName) {
        return safetyIndexRepository.findByCityNameAndDistrictName(cityName, districtName)
                .or(() -> safetyIndexRepository.findByDistrictName(districtName));
    }

    private SafetyIndex toEntity(SafetyIndexCsvRow row) {
        return SafetyIndex.builder()
                .cityName(row.cityName())
                .districtName(row.districtName())
                .trafficAccidentScore(row.trafficAccidentScore())
                .fireScore(row.fireScore())
                .crimeScore(row.crimeScore())
                .lifeSafetyScore(row.lifeSafetyScore())
                .suicideScore(row.suicideScore())
                .infectiousDiseaseScore(row.infectiousDiseaseScore())
                .build();
    }

    private SafetyIndexResponse toResponse(SafetyIndex safetyIndex) {
        return SafetyIndexResponse.builder()
                .overallScore(overallGrade(safetyIndex))
                .lifeSafetyGrade(toGrade(safetyIndex.getLifeSafetyScore()))
                .trafficAccidentGrade(toGrade(safetyIndex.getTrafficAccidentScore()))
                .fireGrade(toGrade(safetyIndex.getFireScore()))
                .crimeGrade(toGrade(safetyIndex.getCrimeScore()))
                .build();
    }

    private String overallGrade(SafetyIndex safetyIndex) {
        return toGrade((int) Math.round(usedAverageScore(safetyIndex)));
    }

    private double usedAverageScore(SafetyIndex safetyIndex) {
        return (safetyIndex.getLifeSafetyScore()
                + safetyIndex.getTrafficAccidentScore()
                + safetyIndex.getFireScore()
                + safetyIndex.getCrimeScore()) / 4.0;
    }

    private String toGrade(int score) {
        if (score <= 1) {
            return "A";
        }
        if (score == 2) {
            return "B";
        }
        if (score == 3) {
            return "C";
        }
        if (score == 4) {
            return "D";
        }
        return "E";
    }
}
