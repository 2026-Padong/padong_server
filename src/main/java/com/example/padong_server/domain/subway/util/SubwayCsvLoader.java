package com.example.padong_server.domain.subway.util;

import com.example.padong_server.domain.subway.entity.Subway;
import com.example.padong_server.domain.subway.repository.SubwayRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SubwayCsvLoader {

    private final SubwayRepository subwayRepository;

    // 운영용 (기본 CSV)
    @Transactional
    public void loadCsv() {
        loadCsv("data/subway/subway.csv");
    }

    // 테스트/확장용
    @Transactional
    public void loadCsv(String path) {
        List<Subway> subways = parseCsv(path);
        subwayRepository.saveAll(subways);
    }

    private List<Subway> parseCsv(String path) {
        List<Subway> subways = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(
                        getClass().getClassLoader().getResourceAsStream(path),
                        Charset.forName("cp949")
                )
        )) {

            String line;
            boolean isFirst = true;

            while ((line = br.readLine()) != null) {
                if (isFirst) {
                    isFirst = false;
                    continue;
                }

                String[] tokens = line.split(",");

                Subway subway = Subway.builder()
                        .line(tokens[1])
                        .stationCode(tokens[2])
                        .stationName(tokens[3])
                        .latitude(Double.parseDouble(tokens[4]))
                        .longitude(Double.parseDouble(tokens[5]))
                        .morningCongestion(Double.parseDouble(tokens[6]))
                        .eveningCongestion(Double.parseDouble(tokens[7]))
                        .build();

                subways.add(subway);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return subways;
    }
}

