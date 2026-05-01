package com.example.padong_server.domain.activityMobility.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.padong_server.domain.activityMobility.dto.ActivityMobilityRepresentativeRow;
import com.example.padong_server.domain.activityMobility.entity.Mobility;
import com.example.padong_server.domain.dongne.dto.AdminDongCsvRow;
import com.example.padong_server.domain.dongne.entity.AdminDong;
import com.example.padong_server.domain.dongne.repository.AdminDongRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ActivityMobilityEntityMapperTest {

    @Mock
    private AdminDongRepository adminDongRepository;

    private ActivityMobilityEntityMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ActivityMobilityEntityMapper(adminDongRepository);
    }

    @Test
    @DisplayName("대표 row의 7자리 행정동 코드를 AdminDong으로 매핑해 Mobility 엔티티로 변환한다")
    void mapsRepresentativeRowsToMobilityEntities() {
        AdminDong arrivalDong = adminDong("1111053000", "종로구", "사직동");
        AdminDong departureDong = adminDong("1162058500", "관악구", "낙성대동");
        when(adminDongRepository.findAll()).thenReturn(List.of(arrivalDong, departureDong));
        ActivityMobilityRepresentativeRow row = representativeRow(
                "1101053",
                "1121058",
                123.45,
                35.6
        );

        List<Mobility> result = mapper.toEntities(List.of(row));

        assertThat(result).hasSize(1);
        Mobility mobility = result.get(0);
        assertThat(mobility)
                .extracting(
                        Mobility::getStartMonth,
                        Mobility::getEndMonth,
                        Mobility::getArrivalDong,
                        Mobility::getDepartureDong,
                        Mobility::getTotalMobility,
                        Mobility::getAvgTime
                )
                .containsExactly("202601", "202603", arrivalDong, departureDong, 123.45, 35.6);
        verify(adminDongRepository).findAll();
    }

    @Test
    @DisplayName("레거시 예외 행정분류 코드는 기존 DongneService와 동일하게 보정한다")
    void appliesLegacyAdminTypeCodeReplacements() {
        AdminDong arrivalDong = adminDong("1150060300", "강서구", "가양제1동");
        AdminDong departureDong = adminDong("1168067500", "강남구", "개포3동");
        when(adminDongRepository.findAll()).thenReturn(List.of(arrivalDong, departureDong));
        ActivityMobilityRepresentativeRow row = representativeRow(
                "1116064",
                "1123074",
                12.3,
                45.6
        );

        List<Mobility> result = mapper.toEntities(List.of(row));

        assertThat(result).singleElement()
                .extracting(Mobility::getArrivalDong, Mobility::getDepartureDong)
                .containsExactly(arrivalDong, departureDong);
    }

    @Test
    @DisplayName("현재 AdminDong에 없는 용신동 생활이동 코드는 용두동 대표 FK로 매핑한다")
    void mapsYongsinDongToYongduDongFallback() {
        AdminDong arrivalDong = adminDong("1123053300", "동대문구", "용두동");
        when(adminDongRepository.findAll()).thenReturn(List.of(arrivalDong));
        ActivityMobilityRepresentativeRow row = representativeRow(
                "1106081",
                "1106081",
                12.3,
                45.6
        );

        List<Mobility> result = mapper.toEntities(List.of(row));

        assertThat(result).singleElement()
                .extracting(Mobility::getArrivalDong, Mobility::getDepartureDong)
                .containsExactly(arrivalDong, arrivalDong);
    }

    @Test
    @DisplayName("매핑할 수 없는 CSV 행정동 코드가 있으면 실패한다")
    void rejectsUnknownAdminDongCode() {
        when(adminDongRepository.findAll()).thenReturn(List.of(adminDong("1111053000", "종로구", "사직동")));
        ActivityMobilityRepresentativeRow row = representativeRow(
                "9999999",
                "1101053",
                12.3,
                45.6
        );

        assertThatThrownBy(() -> mapper.toEntities(List.of(row)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("arrivalDongCode=9999999")
                .hasMessageContaining("생활이동 코드북에 없는 행정동 코드입니다");
    }

    @Test
    @DisplayName("매핑 대상 현재 AdminDong이 DB에 없으면 실패한다")
    void rejectsMissingCurrentAdminDong() {
        when(adminDongRepository.findAll()).thenReturn(List.of());
        ActivityMobilityRepresentativeRow row = representativeRow(
                "1101053",
                "1101053",
                12.3,
                45.6
        );

        assertThatThrownBy(() -> mapper.toEntities(List.of(row)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("생활이동 행정동명에 대응하는 현재 AdminDong이 없습니다")
                .hasMessageContaining("fullName=서울특별시 종로구 사직동");
    }

    @Test
    @DisplayName("CSV 행정동 코드는 7자리 숫자만 허용한다")
    void rejectsInvalidCsvDongCodeFormat() {
        assertThatThrownBy(() -> mapper.normalizeMobilityDongCode("11130750"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("7자리 숫자");
    }

    private ActivityMobilityRepresentativeRow representativeRow(
            String arrivalDongCode,
            String departureDongCode,
            double totalMobility,
            double avgTime
    ) {
        return new ActivityMobilityRepresentativeRow(
                "202601",
                "202603",
                arrivalDongCode,
                departureDongCode,
                totalMobility,
                avgTime,
                3
        );
    }

    private AdminDong adminDong(String adminDongCode, String districtName, String adminDongName) {
        return new AdminDong(new AdminDongCsvRow(
                adminDongCode,
                "서울특별시",
                districtName,
                adminDongName,
                37.5,
                127.0,
                "20081101",
                ""
        ));
    }
}
