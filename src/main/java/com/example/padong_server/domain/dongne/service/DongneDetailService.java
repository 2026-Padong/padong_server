package com.example.padong_server.domain.dongne.service;

import com.example.padong_server.domain.activityMobility.entity.Mobility;
import com.example.padong_server.domain.activityMobility.repository.MobilityRepository;
import com.example.padong_server.domain.dongne.dto.DongneDetailResponse;
import com.example.padong_server.domain.dongne.dto.DongneMobilityResponse;
import com.example.padong_server.domain.dongne.dto.DongneSummaryResponse;
import com.example.padong_server.domain.dongne.entity.AdminDong;
import com.example.padong_server.domain.dongneLike.service.DongneLikeService;
import com.example.padong_server.domain.path.dto.request.PathAllRequest;
import com.example.padong_server.domain.path.dto.response.PathAllResponse;
import com.example.padong_server.domain.path.service.PathService;
import com.example.padong_server.domain.population.entity.Population;
import com.example.padong_server.domain.population.service.PopulationService;
import com.example.padong_server.domain.rentPrice.dto.response.AdminDongRentPriceDetailResponse;
import com.example.padong_server.domain.rentPrice.service.RentPriceService;
import com.example.padong_server.global.ResponseDTO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DongneDetailService {

    private final DongneService dongneService;
    private final MobilityRepository mobilityRepository;
    private final PopulationService populationService;
    private final RentPriceService rentPriceService;
    private final DongneLikeService dongneLikeService;
    private final PathService pathService;

    @Transactional(readOnly = true)
    public ResponseDTO<DongneDetailResponse> getDetail(
            String adminDongCode,
            String arrivalAdminDongCode,
            Long userId
    ) {
        AdminDong selectedDong = dongneService.findAdminDongByCode(adminDongCode);
        AdminDong workDong = hasText(arrivalAdminDongCode)
                ? dongneService.findAdminDongByCode(arrivalAdminDongCode)
                : null;

        Optional<Mobility> mobility = workDong == null
                ? Optional.empty()
                : mobilityRepository.findByArrivalDongAndDepartureDong(workDong, selectedDong);
        Optional<Population> population = populationService.findPopulationByAdmin(selectedDong);
        AdminDongRentPriceDetailResponse rentPrice =
                rentPriceService.getDetail(selectedDong.getAdminDongCode());
        PathAllResponse.Paths paths = resolvePaths(selectedDong, workDong);

        DongneDetailResponse response = DongneDetailResponse.builder()
                .departureDong(toSummary(selectedDong))
                .arrivalDong(workDong == null ? null : toSummary(workDong))
                .mobility(mobility.map(this::toMobilityResponse).orElse(DongneMobilityResponse.empty()))
                .totalPopulation(
                        population.map(Population::getTotalPopulation).map(this::round).orElse(null))
                .density(population.map(Population::getDensity).map(this::round).orElse(null))
                .rentPrice(rentPrice)
                .paths(paths)
                .likeCount(dongneLikeService.getLikeCount(selectedDong.getId()))
                .likedByCurrentUser(dongneLikeService.isLikedByUser(selectedDong.getId(), userId))
                .build();

        return ResponseDTO.res(HttpStatus.OK, "동네 상세 조회 성공", response);
    }

    private DongneSummaryResponse toSummary(AdminDong adminDong) {
        return DongneSummaryResponse.builder()
                .adminDongCode(adminDong.getAdminDongCode())
                .cityName(adminDong.getCityName())
                .districtName(adminDong.getDistrictName())
                .adminDongName(adminDong.getAdminDongName())
                .address(
                        adminDong.getCityName()
                                + " "
                                + adminDong.getDistrictName()
                                + " "
                                + adminDong.getAdminDongName())
                .latitude(adminDong.getLatitude())
                .longitude(adminDong.getLongitude())
                .build();
    }

    private DongneMobilityResponse toMobilityResponse(Mobility mobility) {
        return DongneMobilityResponse.builder()
                .totalMobility(round(mobility.getTotalMobility()))
                .avgTime(round(mobility.getAvgTime()))
                .startMonth(mobility.getStartMonth())
                .endMonth(mobility.getEndMonth())
                .build();
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private PathAllResponse.Paths resolvePaths(AdminDong selectedDong, AdminDong workDong) {
        if (workDong == null
                || workDong.getAdminDongCode().equals(selectedDong.getAdminDongCode())) {
            return null;
        }
        return pathService
                .searchAll(
                        PathAllRequest.builder()
                                .departureDongCode(selectedDong.getAdminDongCode())
                                .arrivalDongCode(workDong.getAdminDongCode())
                                .build())
                .getPaths();
    }
}
