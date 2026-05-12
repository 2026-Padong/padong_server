package com.example.padong_server.domain.activityMobility.service;

import com.example.padong_server.domain.activityMobility.entity.SafetyIndex;
import com.example.padong_server.domain.activityMobility.repository.SafetyIndexRepository;
import com.example.padong_server.domain.activityMobility.util.SafetyIndexDataUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class SafetyIndexServiceTest {

    @Mock
    private SafetyIndexRepository safetyIndexRepository;

    @Mock
    private SafetyIndexDataUtil safetyIndexDataUtil;

    @InjectMocks
    private SafetyIndexService safetyIndexService;

    @Test
    void findsSafetyResponseByCityAndDistrict() {
        given(safetyIndexRepository.findByCityNameAndDistrictName("서울", "마포구"))
                .willReturn(Optional.of(
                        SafetyIndex.builder()
                                .cityName("서울")
                                .districtName("마포구")
                                .trafficAccidentScore(5)
                                .fireScore(4)
                                .crimeScore(4)
                                .lifeSafetyScore(3)
                                .suicideScore(2)
                                .infectiousDiseaseScore(3)
                                .build()));

        var response = safetyIndexService.findResponse("서울", "마포구");

        assertThat(response).isPresent();
        assertThat(response.get().overallScore()).isEqualTo("D");
        assertThat(response.get().lifeSafetyGrade()).isEqualTo("C");
        assertThat(response.get().trafficAccidentGrade()).isEqualTo("E");
        assertThat(response.get().fireGrade()).isEqualTo("D");
        assertThat(response.get().crimeGrade()).isEqualTo("D");
    }
}
