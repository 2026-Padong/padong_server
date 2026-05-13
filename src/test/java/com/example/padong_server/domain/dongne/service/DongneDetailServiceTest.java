package com.example.padong_server.domain.dongne.service;

import com.example.padong_server.domain.activityMobility.dto.SafetyIndexResponse;
import com.example.padong_server.domain.activityMobility.entity.Mobility;
import com.example.padong_server.domain.activityMobility.repository.MobilityRepository;
import com.example.padong_server.domain.activityMobility.service.SafetyIndexService;
import com.example.padong_server.domain.dongne.dto.AdminDongCsvRow;
import com.example.padong_server.domain.dongne.dto.DongneDetailResponse;
import com.example.padong_server.domain.dongne.entity.AdminDong;
import com.example.padong_server.domain.dongneLike.service.DongneLikeService;
import com.example.padong_server.domain.path.dto.response.PathAllResponse;
import com.example.padong_server.domain.path.service.PathService;
import com.example.padong_server.domain.population.entity.Population;
import com.example.padong_server.domain.population.service.PopulationService;
import com.example.padong_server.domain.rentPrice.dto.response.AdminDongRentPriceDetailResponse;
import com.example.padong_server.domain.rentPrice.service.RentPriceService;
import com.example.padong_server.global.ResponseDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class DongneDetailServiceTest {

    @Mock
    private DongneService dongneService;

    @Mock
    private MobilityRepository mobilityRepository;

    @Mock
    private PopulationService populationService;

    @Mock
    private RentPriceService rentPriceService;

    @Mock
    private DongneLikeService dongneLikeService;

    @Mock
    private PathService pathService;

    @Mock
    private SafetyIndexService safetyIndexService;

    @Mock
    private com.example.padong_server.domain.dongne.boundary.AdminDongBoundaryService boundaryService;

    @Mock
    private com.example.padong_server.domain.picture.repository.TourPictureRepository tourPictureRepository;

    @InjectMocks
    private DongneDetailService dongneDetailService;

    @Test
    void returnsSafetyInformationInDongneDetail() {
        AdminDong selectedDong = adminDong("1144066000", "마포구", "연남동");
        AdminDong workDong = adminDong("1168064000", "강남구", "역삼1동");
        Mobility mobility = Mobility.builder()
                .totalMobility(1200.5)
                .avgTime(35.4)
                .startMonth("202601")
                .endMonth("202603")
                .arrivalDong(workDong)
                .departureDong(selectedDong)
                .build();
        Population population = population(12340.0, 8920.0);

        given(dongneService.findAdminDongByCode("1144066000")).willReturn(selectedDong);
        given(dongneService.findAdminDongByCode("1168064000")).willReturn(workDong);
        given(mobilityRepository.findByArrivalDongAndDepartureDong(workDong, selectedDong))
                .willReturn(Optional.of(mobility));
        given(populationService.findPopulationByAdmin(selectedDong)).willReturn(Optional.of(population));
        given(safetyIndexService.findResponse("서울", "마포구"))
                .willReturn(Optional.of(
                        SafetyIndexResponse.builder()
                                .overallScore("C")
                                .lifeSafetyGrade("C")
                                .trafficAccidentGrade("B")
                                .fireGrade("C")
                                .crimeGrade("D")
                                .build()));
        given(rentPriceService.getDetail("1144066000"))
                .willReturn(new AdminDongRentPriceDetailResponse(
                        "1144066000", "최근 2년 기준", null, null, true, null, List.of()));
        given(pathService.searchAll(org.mockito.ArgumentMatchers.any()))
                .willReturn(PathAllResponse.builder().build());
        given(dongneLikeService.getLikeCount(1L)).willReturn(3L);
        given(dongneLikeService.isLikedByUser(1L, 1L)).willReturn(true);

        ResponseDTO<DongneDetailResponse> response =
                dongneDetailService.getDetail("1144066000", "1168064000", 1L);

        assertThat(response.getStatusCode()).isEqualTo(String.valueOf(HttpStatus.OK.value()));
        assertThat(response.getData().getSafety()).isNotNull();
        assertThat(response.getData().getSafety().overallScore()).isEqualTo("C");
        assertThat(response.getData().getSafety().trafficAccidentGrade()).isEqualTo("B");
        assertThat(response.getData().getSafety().lifeSafetyGrade()).isEqualTo("C");
    }

    private AdminDong adminDong(String code, String districtName, String adminDongName) {
        AdminDong adminDong = new AdminDong(new AdminDongCsvRow(
                code, "서울", districtName, adminDongName, 37.5, 127.0, "20081101", ""));
        setField(adminDong, "id", 1L);
        return adminDong;
    }

    private Population population(double totalPopulation, double density) {
        Population population = new Population();
        setField(population, "totalPopulation", totalPopulation);
        setField(population, "density", density);
        return population;
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
