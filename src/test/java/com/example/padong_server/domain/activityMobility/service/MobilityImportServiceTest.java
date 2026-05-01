package com.example.padong_server.domain.activityMobility.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.padong_server.domain.activityMobility.dto.ActivityMobilityCsvRow;
import com.example.padong_server.domain.activityMobility.dto.ActivityMobilityRepresentativeRow;
import com.example.padong_server.domain.activityMobility.entity.Mobility;
import com.example.padong_server.domain.activityMobility.repository.MobilityRepository;
import com.example.padong_server.domain.activityMobility.util.ActivityMobilityDataUtil;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MobilityImportServiceTest {

    @Mock
    private ActivityMobilityDataUtil activityMobilityDataUtil;

    @Mock
    private ActivityMobilityAggregator activityMobilityAggregator;

    @Mock
    private ActivityMobilityEntityMapper activityMobilityEntityMapper;

    @Mock
    private MobilityRepository mobilityRepository;

    @InjectMocks
    private MobilityImportService mobilityImportService;

    @Test
    @DisplayName("CSV 읽기부터 대표값 저장까지 수행하고 완료 안내 문구를 반환한다")
    void importsActivityMobilityData() {
        List<ActivityMobilityCsvRow> csvRows = List.of(
                csvRow("202603", "1113075", "1121058"),
                csvRow("202601", "1113075", "1121058"),
                csvRow("202602", "1113075", "1113075")
        );
        List<ActivityMobilityRepresentativeRow> representativeRows = List.of(
                representativeRow("1113075", "1113075"),
                representativeRow("1113075", "1121058")
        );
        List<Mobility> mobilities = List.of(Mobility.builder().build(), Mobility.builder().build());
        when(activityMobilityDataUtil.readMonthlyCsvRows()).thenReturn(csvRows);
        when(activityMobilityAggregator.aggregate(csvRows, "202601", "202603")).thenReturn(representativeRows);
        when(activityMobilityEntityMapper.toEntities(representativeRows)).thenReturn(mobilities);
        when(mobilityRepository.saveAll(mobilities)).thenReturn(mobilities);

        String result = mobilityImportService.importData();

        assertThat(result).isEqualTo("생활이동 데이터 적재 완료: 202601~202603 기준, 2건 저장");
        InOrder inOrder = inOrder(mobilityRepository);
        inOrder.verify(mobilityRepository).deleteAllInBatch();
        inOrder.verify(mobilityRepository).saveAll(mobilities);
    }

    @Test
    @DisplayName("CSV row가 비어 있으면 기존 Mobility를 삭제하지 않고 실패한다")
    void rejectsEmptyCsvRowsBeforeReplacingData() {
        when(activityMobilityDataUtil.readMonthlyCsvRows()).thenReturn(List.of());

        assertThatThrownBy(() -> mobilityImportService.importData())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("생활이동 CSV row가 없습니다");

        verifyNoInteractions(activityMobilityAggregator, activityMobilityEntityMapper, mobilityRepository);
    }

    private ActivityMobilityCsvRow csvRow(String month, String arrivalDongCode, String departureDongCode) {
        return new ActivityMobilityCsvRow(
                month,
                arrivalDongCode,
                departureDongCode,
                90.0,
                100.0,
                50.0,
                30.0
        );
    }

    private ActivityMobilityRepresentativeRow representativeRow(String arrivalDongCode, String departureDongCode) {
        return new ActivityMobilityRepresentativeRow(
                "202601",
                "202603",
                arrivalDongCode,
                departureDongCode,
                90.0,
                30.0,
                3
        );
    }
}
