package com.example.padong_server.domain.subway.util;

import com.example.padong_server.domain.subway.entity.Subway;
import com.example.padong_server.domain.subway.repository.SubwayRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@WebMvcTest
class SubwayCsvLoaderTest {

    @Autowired
    private SubwayCsvLoader subwayCsvLoader;

    @Autowired
    private SubwayRepository subwayRepository;

    @Test
    @DisplayName("CSV 로드 후 지하철 데이터 저장")
    void load_and_save() {
            subwayRepository.deleteAll();

            subwayCsvLoader.loadCsv("data/test_subway.csv");

            List<Subway> subways = subwayRepository.findAll();

            // 1. 개수 확인
            assertThat(subways).hasSize(10);

        assertThat(subways).isNotEmpty();

        Subway first = subways.get(0);

        assertThat(first.getLine()).isNotBlank();
        assertThat(first.getStationCode()).isNotBlank();
        assertThat(first.getStationName()).isNotBlank();

        assertThat(first.getMorningCongestion()).isNotNull();
        assertThat(first.getEveningCongestion()).isNotNull();

        assertThat(first.getLatitude()).isNotNull();
        assertThat(first.getLongitude()).isNotNull();
        }
}