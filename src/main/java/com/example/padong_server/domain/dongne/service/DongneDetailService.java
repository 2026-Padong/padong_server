package com.example.padong_server.domain.dongne.service;

import com.example.padong_server.domain.activityMobility.dto.SafetyIndexResponse;
import com.example.padong_server.domain.activityMobility.entity.Mobility;
import com.example.padong_server.domain.activityMobility.repository.MobilityRepository;
import com.example.padong_server.domain.activityMobility.service.SafetyIndexService;
import com.example.padong_server.domain.dongne.dto.DongneDetailResponse;
import com.example.padong_server.domain.dongne.dto.DongneMobilityResponse;
import com.example.padong_server.domain.dongne.dto.DongneSummaryResponse;
import com.example.padong_server.domain.dongne.entity.AdminDong;
import com.example.padong_server.domain.dongneLike.service.DongneLikeService;
import com.example.padong_server.domain.path.dto.internal.PathSummary;
import com.example.padong_server.domain.path.dto.request.PathAllRequest;
import com.example.padong_server.domain.path.dto.response.PathAllResponse;
import com.example.padong_server.domain.path.service.PathService;
import com.example.padong_server.domain.picture.entity.TourPicture;
import com.example.padong_server.domain.picture.repository.TourPictureRepository;
import com.example.padong_server.domain.population.entity.Population;
import com.example.padong_server.domain.population.service.PopulationService;
import com.example.padong_server.domain.rentPrice.dto.response.AdminDongRentPriceDetailResponse;
import com.example.padong_server.domain.rentPrice.service.RentPriceService;
import com.example.padong_server.global.ResponseDTO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import com.example.padong_server.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DongneDetailService {

    private final DongneService dongneService;
    private final MobilityRepository mobilityRepository;
    private final PopulationService populationService;
    private final RentPriceService rentPriceService;
    private final DongneLikeService dongneLikeService;
    private final PathService pathService;
    private final SafetyIndexService safetyIndexService;
    private final TourPictureRepository tourPictureRepository;

    // PathService.searchAll 이 path_record 캐시 upsert 를 수행하므로 readOnly 트랜잭션으로 묶을 수 없음.
    // 캐시 쓰기를 trigger 하는 read 라 사실상 read-only 아님.
    @Transactional
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
        SafetyIndexResponse safety = safetyIndexService
                .findResponse(selectedDong.getCityName(), selectedDong.getDistrictName())
                .orElse(null);
        AdminDongRentPriceDetailResponse rentPrice =
                rentPriceService.getDetail(selectedDong.getAdminDongCode());
        PathAllResponse.Paths paths = resolvePaths(selectedDong, workDong);
        List<String> images =
                tourPictureRepository
                        .findByAdminDongCodeOrderByTitleAscContentIdAsc(
                                selectedDong.getAdminDongCode())
                        .stream()
                        .map(TourPicture::getFirstImageUrl)
                        .filter(url -> url != null && !url.isBlank())
                        .toList();

        DongneDetailResponse response = DongneDetailResponse.builder()
                .departureDong(toSummary(selectedDong))
                .arrivalDong(workDong == null ? null : toSummary(workDong))
                .mobility(mobility.map(this::toMobilityResponse).orElse(DongneMobilityResponse.empty()))
                .totalPopulation(
                        population.map(Population::getTotalPopulation).map(this::round).orElse(null))
                .density(population.map(Population::getDensity).map(this::round).orElse(null))
                .safety(safety)
                .rentPrice(rentPrice)
                .paths(paths)
                .images(images)
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
        if (workDong == null) {
            return null;
        }
        if (workDong.getAdminDongCode().equals(selectedDong.getAdminDongCode())) {
            PathSummary zero = new PathSummary(0, 0, null);
            return PathAllResponse.Paths.builder()
                    .transit(zero)
                    .pedestrian(zero)
                    .car(zero)
                    .build();
        }
        // PathService 가 외부 인프라 예외를 CustomException 으로 정규화해서 던짐.
        // 키 미설정·700m 이내·결과 없음·외부 4xx/5xx 등 어떤 실패든 paths 만 null 로 떨어뜨리고
        // detail 의 나머지 필드 (인구·임대료·안전·좋아요) 는 그대로 살림.
        try {
            return pathService
                    .searchAll(
                            PathAllRequest.builder()
                                    .departureDongCode(selectedDong.getAdminDongCode())
                                    .arrivalDongCode(workDong.getAdminDongCode())
                                    .build())
                    .getPaths();
        } catch (CustomException e) {
            log.warn(
                    "동네 상세 paths 조회 실패 — paths=null 로 대체. departure={}, arrival={}, code={}, msg={}",
                    selectedDong.getAdminDongCode(),
                    workDong.getAdminDongCode(),
                    e.getErrorCode(),
                    e.getMessage());
            return null;
        }
    }
}
