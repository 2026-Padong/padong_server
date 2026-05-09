package com.example.padong_server.domain.subway.util;

import com.example.padong_server.domain.subway.entity.Subway;
import com.example.padong_server.domain.subway.repository.SubwayRepository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled("테스트 DB(H2 등) 미구성으로 ApplicationContext 로드 실패. test 프로파일/DB 설정 후 활성화")
@SpringBootTest
class SubwayCsvLoaderTest {

    @Autowired
    private SubwayCsvLoader subwayCsvLoader;

    @Autowired
    private SubwayRepository subwayRepository;

    @Test
    @DisplayName("CSV를 로드하면 지하철 데이터가 저장된다")
    void load_and_save() {
        subwayRepository.deleteAll();

        subwayCsvLoader.loadCsv("data/subway/test_subway.csv");

        List<Subway> subways = subwayRepository.findAll();

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
