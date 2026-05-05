package com.example.padong_server.domain.activityMobility.service;

import com.example.padong_server.domain.activityMobility.dto.CommonDepartureMobilityResponse;
import com.example.padong_server.domain.activityMobility.dto.IntersectedMobilityResponse;
import com.example.padong_server.domain.activityMobility.dto.MobilityFilterRequest;
import com.example.padong_server.domain.activityMobility.dto.MobilityResponse;
import com.example.padong_server.domain.activityMobility.dto.MobilitySimpleResponse;
import com.example.padong_server.domain.activityMobility.dto.MultiMobilityResponse;
import com.example.padong_server.domain.activityMobility.entity.Mobility;
import com.example.padong_server.domain.activityMobility.repository.MobilityRepository;
import com.example.padong_server.domain.activityMobility.support.CommonDepartureAccumulator;
import com.example.padong_server.domain.dongne.dto.AdminDongDto;
import com.example.padong_server.domain.dongne.entity.AdminDong;
import com.example.padong_server.domain.dongne.service.DongneService;
import com.example.padong_server.domain.population.service.PopulationService;
import com.example.padong_server.domain.rentPrice.dto.request.RentPriceFilterCriteria;
import com.example.padong_server.domain.rentPrice.dto.response.AdminDongRentPriceSummaryResponse;
import com.example.padong_server.domain.rentPrice.dto.response.SelectedRentPriceResponse;
import com.example.padong_server.domain.rentPrice.service.RentPriceService;
import com.example.padong_server.domain.score.service.ScoreCalculator;
import com.example.padong_server.global.PageResponse;
import com.example.padong_server.global.ResponseDTO;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MobilityService {

    private static final String MOBILITY_NOT_FOUND_MESSAGE = "해당 생활이동 데이터가 존재하지 않습니다.";
    private static final String FILTERED_MOBILITY_NOT_FOUND_MESSAGE = "조건에 맞는 생활이동 데이터가 존재하지 않습니다.";
    private static final String MINIMUM_ADMIN_DONG_CODE_COUNT_MESSAGE = "행정동 코드는 2개 이상 입력해야 합니다.";
    private static final String COMMON_MOBILITY_NOT_FOUND_MESSAGE = "공통 생활이동 데이터가 존재하지 않습니다.";
    private static final String LEGACY_ADMIN_DONG_NOT_FOUND_MESSAGE = "해당 행정동이 존재하지 않습니다.";

    private final MobilityRepository mobilityRepository;
    private final DongneService dongneService;
    private final PopulationService populationService;
    private final RentPriceService rentPriceService;
    private final ScoreCalculator scoreCalculator;

    public ResponseDTO<PageResponse<MobilitySimpleResponse>> searchByArrivalDongCode(
            String adminDongCode, Pageable pageable) {
        AdminDong adminDong = dongneService.findAdminDongByCode(adminDongCode);
        Page<Mobility> mobilities = mobilityRepository.findByArrivalDong(adminDong, pageable);
        if (mobilities.getTotalElements() == 0) {
            return ResponseDTO.res(HttpStatus.NOT_FOUND, MOBILITY_NOT_FOUND_MESSAGE);
        }

        Map<String, SelectedRentPriceResponse> rentPriceByAdminDongCode =
                rentPriceService.getSelectedRentPrices(
                        adminDongCodes(
                                mobilities.getContent().stream()
                                        .map(Mobility::getDepartureDong)
                                        .toList()),
                        RentPriceFilterCriteria.empty());

        return ResponseDTO.res(
                HttpStatus.OK,
                "생활이동 많은 순 조회 성공",
                PageResponse.from(
                        mobilities,
                        mapToSimpleResponses(mobilities.getContent(), rentPriceByAdminDongCode)));
    }

    public ResponseDTO<PageResponse<MobilitySimpleResponse>> searchByArrivalDongCode(
            String adminDongCode, Pageable pageable, MobilityFilterRequest filterRequest) {
        MobilityFilterRequest filter = MobilityFilterRequest.emptyIfNull(filterRequest);
        if (!filter.hasSingleFilter()) {
            return searchByArrivalDongCode(adminDongCode, pageable);
        }

        AdminDong adminDong = dongneService.findAdminDongByCode(adminDongCode);
        List<Mobility> mobilities = mobilityRepository.findByArrivalDong(adminDong);
        if (mobilities.isEmpty()) {
            return ResponseDTO.res(HttpStatus.NOT_FOUND, MOBILITY_NOT_FOUND_MESSAGE);
        }

        List<Mobility> filteredMobilities = filterSingleMobilities(mobilities, filter);
        if (filteredMobilities.isEmpty()) {
            return ResponseDTO.res(HttpStatus.NOT_FOUND, FILTERED_MOBILITY_NOT_FOUND_MESSAGE);
        }

        List<Mobility> pageMobilities = listToPage(filteredMobilities, pageable);
        Map<String, SelectedRentPriceResponse> rentPriceByAdminDongCode =
                rentPriceService.getSelectedRentPrices(
                        adminDongCodes(
                                pageMobilities.stream().map(Mobility::getDepartureDong).toList()),
                        filter.toRentPriceFilterCriteria());

        return ResponseDTO.res(
                HttpStatus.OK,
                "생활이동 많은 순 조회 성공",
                PageResponse.of(
                        mapToSimpleResponses(pageMobilities, rentPriceByAdminDongCode),
                        pageable,
                        filteredMobilities.size()));
    }

    public ResponseDTO<PageResponse<CommonDepartureMobilityResponse>> searchByArrivalDongCodes(
            List<String> adminDongCodes, Pageable pageable) {
        return searchByArrivalDongCodes(adminDongCodes, pageable, MobilityFilterRequest.empty());
    }

    public ResponseDTO<PageResponse<CommonDepartureMobilityResponse>> searchByArrivalDongCodes(
            List<String> adminDongCodes, Pageable pageable, MobilityFilterRequest filterRequest) {
        MobilityFilterRequest filter = MobilityFilterRequest.emptyIfNull(filterRequest);
        List<String> distinctAdminDongCodes = normalizeAdminDongCodes(adminDongCodes);
        if (distinctAdminDongCodes.size() < 2) {
            return ResponseDTO.res(HttpStatus.BAD_REQUEST, MINIMUM_ADMIN_DONG_CODE_COUNT_MESSAGE);
        }

        List<AdminDong> arrivalDongs =
                distinctAdminDongCodes.stream().map(dongneService::findAdminDongByCode).toList();
        List<Mobility> mobilities = mobilityRepository.findByArrivalDongIn(arrivalDongs);
        PageResponse<CommonDepartureMobilityResponse> responses =
                mapToCommonDepartureResponses(
                        mobilities, distinctAdminDongCodes.size(), pageable, filter);
        if (responses.totalElements() == 0) {
            return ResponseDTO.res(HttpStatus.NOT_FOUND, COMMON_MOBILITY_NOT_FOUND_MESSAGE);
        }

        return ResponseDTO.res(HttpStatus.OK, "다중 행정동 생활이동 많은 순 조회 성공", responses);
    }

    private List<String> normalizeAdminDongCodes(List<String> adminDongCodes) {
        if (adminDongCodes == null) {
            return List.of();
        }
        return adminDongCodes.stream()
                .map(code -> code == null ? "" : code.trim())
                .filter(code -> !code.isBlank())
                .distinct()
                .toList();
    }

    private List<String> adminDongCodes(Collection<AdminDong> adminDongs) {
        return adminDongs.stream().map(AdminDong::getAdminDongCode).distinct().toList();
    }

    private List<Mobility> filterSingleMobilities(
            List<Mobility> mobilities, MobilityFilterRequest filter) {
        Set<String> districtNames = normalizeDistrictNames(filter.getDepartureDistrictNames());
        List<Mobility> filteredMobilities =
                mobilities.stream()
                        .filter(
                                mobility ->
                                        matchesRange(
                                                mobility.getAvgTime(),
                                                filter.getMinAvgTime(),
                                                filter.getMaxAvgTime()))
                        .filter(
                                mobility ->
                                        districtNames.isEmpty()
                                                || districtNames.contains(
                                                        mobility.getDepartureDong()
                                                                .getDistrictName()))
                        .sorted(
                                Comparator.comparingDouble(Mobility::getTotalMobility)
                                        .reversed()
                                        .thenComparing(
                                                mobility ->
                                                        mobility.getDepartureDong()
                                                                .getAdminDongCode()))
                        .toList();
        return filterMobilitiesByRentPrice(filteredMobilities, filter);
    }

    private List<Mobility> filterMultiMobilities(
            List<Mobility> mobilities, MobilityFilterRequest filter) {
        Set<String> districtNames = normalizeDistrictNames(filter.getDepartureDistrictNames());
        return mobilities.stream()
                .filter(
                        mobility ->
                                matchesRange(
                                        mobility.getAvgTime(),
                                        filter.getMinEachAvgTime(),
                                        filter.getMaxEachAvgTime()))
                .filter(
                        mobility ->
                                districtNames.isEmpty()
                                        || districtNames.contains(
                                                mobility.getDepartureDong().getDistrictName()))
                .toList();
    }

    private List<Mobility> filterMobilitiesByRentPrice(
            List<Mobility> mobilities, MobilityFilterRequest filter) {
        if (!filter.hasRentPriceFilter() || mobilities.isEmpty()) {
            return mobilities;
        }
        Set<String> matchedAdminDongCodes =
                rentPriceService.findMatchedAdminDongCodes(
                        adminDongCodes(
                                mobilities.stream().map(Mobility::getDepartureDong).toList()),
                        filter.toRentPriceFilterCriteria());
        return mobilities.stream()
                .filter(
                        mobility ->
                                matchedAdminDongCodes.contains(
                                        mobility.getDepartureDong().getAdminDongCode()))
                .toList();
    }

    private List<CommonDepartureAccumulator> filterMultiAccumulatorsByRentPrice(
            List<CommonDepartureAccumulator> accumulators, MobilityFilterRequest filter) {
        if (!filter.hasRentPriceFilter() || accumulators.isEmpty()) {
            return accumulators;
        }
        Set<String> matchedAdminDongCodes =
                rentPriceService.findMatchedAdminDongCodes(
                        adminDongCodes(
                                accumulators.stream()
                                        .map(CommonDepartureAccumulator::departureDong)
                                        .toList()),
                        filter.toRentPriceFilterCriteria());
        return accumulators.stream()
                .filter(
                        accumulator ->
                                matchedAdminDongCodes.contains(
                                        accumulator.departureDong().getAdminDongCode()))
                .toList();
    }

    private boolean matchesRange(double value, Double min, Double max) {
        if (min != null && value < min) {
            return false;
        }
        return max == null || value <= max;
    }

    private Set<String> normalizeDistrictNames(List<String> districtNames) {
        if (districtNames == null) {
            return Set.of();
        }
        return districtNames.stream()
                .map(name -> name == null ? "" : name.trim())
                .filter(name -> !name.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private PageResponse<CommonDepartureMobilityResponse> mapToCommonDepartureResponses(
            List<Mobility> mobilities,
            int requiredArrivalDongCount,
            Pageable pageable,
            MobilityFilterRequest filter) {
        Map<String, CommonDepartureAccumulator> accumulatorByDepartureDongCode =
                new LinkedHashMap<>();
        for (Mobility mobility : filterMultiMobilities(mobilities, filter)) {
            String departureDongCode = mobility.getDepartureDong().getAdminDongCode();
            accumulatorByDepartureDongCode
                    .computeIfAbsent(
                            departureDongCode,
                            ignored -> new CommonDepartureAccumulator(mobility.getDepartureDong()))
                    .add(mobility);
        }

        List<CommonDepartureAccumulator> accumulators =
                accumulatorByDepartureDongCode.values().stream()
                        .filter(
                                accumulator ->
                                        accumulator.arrivalDongCount() == requiredArrivalDongCount)
                        .toList();
        accumulators = filterMultiAccumulatorsByRentPrice(accumulators, filter);

        List<CommonDepartureAccumulator> sortedAccumulators =
                accumulators.stream()
                        .sorted(
                                Comparator.comparingDouble(
                                                CommonDepartureAccumulator::totalMobility)
                                        .reversed()
                                        .thenComparing(
                                                accumulator ->
                                                        accumulator
                                                                .departureDong()
                                                                .getAdminDongCode()))
                        .toList();
        List<CommonDepartureAccumulator> pageAccumulators =
                listToPage(sortedAccumulators, pageable);
        Map<String, SelectedRentPriceResponse> rentPriceByAdminDongCode =
                rentPriceService.getSelectedRentPrices(
                        adminDongCodes(
                                pageAccumulators.stream()
                                        .map(CommonDepartureAccumulator::departureDong)
                                        .toList()),
                        filter.toRentPriceFilterCriteria());
        List<CommonDepartureMobilityResponse> responses =
                pageAccumulators.stream()
                        .map(
                                accumulator ->
                                        CommonDepartureMobilityResponse.from(
                                                accumulator.departureDong(),
                                                accumulator.totalMobility(),
                                                rentPriceByAdminDongCode.get(
                                                        accumulator
                                                                .departureDong()
                                                                .getAdminDongCode())))
                        .toList();
        return PageResponse.of(responses, pageable, sortedAccumulators.size());
    }

    private List<MobilitySimpleResponse> mapToSimpleResponses(
            Collection<Mobility> mobilities,
            Map<String, SelectedRentPriceResponse> rentPriceByAdminDongCode) {
        return mobilities.stream()
                .map(
                        mobility ->
                                MobilitySimpleResponse.from(
                                        mobility,
                                        rentPriceByAdminDongCode.get(
                                                mobility.getDepartureDong().getAdminDongCode())))
                .toList();
    }

    private <T> List<T> listToPage(List<T> list, Pageable pageable) {
        int totalElements = list.size();
        int fromIndex = Math.toIntExact(pageable.getOffset());
        int toIndex = Math.min(fromIndex + pageable.getPageSize(), totalElements);

        if (fromIndex < totalElements) {
            return list.subList(fromIndex, toIndex);
        }
        return Collections.emptyList();
    }

    // 사용 중단된 주소 기반 레거시 API 및 헬퍼
    @Deprecated
    public List<MobilityResponse> mapFromEntities(Collection<Mobility> mobilities) {
        Map<String, AdminDongRentPriceSummaryResponse> rentPriceSummaryByAdminDongCode =
                rentPriceService
                        .getSummaries(
                                mobilities.stream()
                                        .map(Mobility::getDepartureDong)
                                        .map(AdminDong::getAdminDongCode)
                                        .distinct()
                                        .toList(),
                                null,
                                null)
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        AdminDongRentPriceSummaryResponse::adminDongCode,
                                        Function.identity(),
                                        (a, b) -> a));

        List<MobilityResponse> responses = new ArrayList<>();
        for (Mobility mobility : mobilities) {
            AdminDong departureDong = mobility.getDepartureDong();
            // legacy
            // double safety = departureDong.getSafetyGrade().getAvgGrade();
            double safety = 0.0;
            double density = populationService.getPopulationByAdmin(departureDong).getDensity();
            double avgTime = mobility.getAvgTime();
            double totalMobility = mobility.getTotalMobility();
            AdminDongRentPriceSummaryResponse rentPriceSummary =
                    rentPriceSummaryByAdminDongCode.get(departureDong.getAdminDongCode());
            double avgJeonseDeposit = avgJeonseDeposit(rentPriceSummary);
            double avgMonthlyDeposit = avgMonthlyDeposit(rentPriceSummary);
            double avgMonthlyRent = avgMonthlyRent(rentPriceSummary);
            double score = scoreCalculator.calculateScore(totalMobility, avgTime, density, safety);

            responses.add(
                    MobilityResponse.builder()
                            .departureDong(AdminDongDto.from(departureDong))
                            .totalMobility(Math.round(totalMobility * 100) / 100.0)
                            .avgTime(Math.round(avgTime * 100) / 100.0)
                            .density(Math.round(density * 100) / 100.0)
                            .safety(Math.round(safety * 100) / 100.0)
                            .avgJeonseDeposit(Math.round(avgJeonseDeposit * 100) / 100.0)
                            .avgMonthlyDeposit(Math.round(avgMonthlyDeposit * 100) / 100.0)
                            .avgMonthlyRent(Math.round(avgMonthlyRent * 100) / 100.0)
                            .score(score)
                            .build());
        }
        return responses;
    }

    @Deprecated
    public ResponseDTO<List<MobilityResponse>> searchByAddress(String address, Pageable pageable) {
        AdminDong adminDong = dongneService.findAdminDongByAddress(address);
        Page<Mobility> mobilities = mobilityRepository.findByArrivalDong(adminDong, pageable);
        if (mobilities.isEmpty()) {
            return ResponseDTO.res(HttpStatus.NOT_FOUND, LEGACY_ADMIN_DONG_NOT_FOUND_MESSAGE);
        }

        return ResponseDTO.res(
                HttpStatus.OK, "출발 행정동 조회 성공", mapFromEntities(mobilities.getContent()));
    }

    @Deprecated
    public Mobility findByGeoCodes(String arrivalCode, String departureCode) {
        AdminDong arrivalDong = dongneService.findAdminDongByCode(arrivalCode);
        AdminDong departureDong = dongneService.findAdminDongByCode(departureCode);
        Optional<Mobility> optional =
                mobilityRepository.findByArrivalDongAndDepartureDong(arrivalDong, departureDong);
        return optional.orElse(null);
    }

    @Deprecated
    public ResponseDTO<MultiMobilityResponse> searchByTwoAddresses(
            String address1, String address2, int page) {
        AdminDong adminDong1 = dongneService.findAdminDongByAddress(address1);
        AdminDong adminDong2 = dongneService.findAdminDongByAddress(address2);

        List<MobilityResponse> firstMobility =
                mapFromEntities(mobilityRepository.findByArrivalDong(adminDong1));
        List<MobilityResponse> secondMobility =
                mapFromEntities(mobilityRepository.findByArrivalDong(adminDong2));

        Pageable pageable = PageRequest.of(page, 10, Sort.by(Sort.Direction.DESC, "totalMobility"));
        List<MobilityResponse> first = searchByAddress(address1, pageable).getData();
        List<MobilityResponse> second = searchByAddress(address2, pageable).getData();

        Map<String, MobilityResponse> map1 =
                firstMobility.stream()
                        .collect(
                                Collectors.toMap(
                                        r -> r.getDepartureDong().getAdminDongCode(),
                                        Function.identity(),
                                        (a, b) -> a));

        Map<String, MobilityResponse> map2 =
                secondMobility.stream()
                        .collect(
                                Collectors.toMap(
                                        r -> r.getDepartureDong().getAdminDongCode(),
                                        Function.identity(),
                                        (a, b) -> a));

        List<IntersectedMobilityResponse> intersected = new ArrayList<>();

        for (String code : map1.keySet()) {
            if (map2.containsKey(code)) {
                MobilityResponse r1 = map1.get(code);
                MobilityResponse r2 = map2.get(code);

                intersected.add(
                        IntersectedMobilityResponse.builder()
                                .departureDong(r1.getDepartureDong())
                                .score(r1.getScore())
                                .density(r1.getDensity())
                                .safety(r1.getSafety())
                                .avgMonthlyDeposit(r1.getAvgMonthlyDeposit())
                                .avgMonthlyRent(r1.getAvgMonthlyRent())
                                .avgJeonseDeposit(r1.getAvgJeonseDeposit())
                                .totalMobility1(r1.getTotalMobility())
                                .totalMobility2(r2.getTotalMobility())
                                .avgTime1(r1.getAvgTime())
                                .avgTime2(r2.getAvgTime())
                                .build());
            }
        }

        // intersected 필터링 조건 유지 (필요 없으면 이 블록 삭제 가능)
        intersected =
                intersected.stream()
                        .filter(
                                r ->
                                        r.getTotalMobility1() >= 10
                                                && r.getTotalMobility2() >= 10
                                                && (r.getTotalMobility1() + r.getTotalMobility2()
                                                        >= 50)
                                                && r.getAvgTime1() >= 20
                                                && r.getAvgTime2() >= 20)
                        .collect(Collectors.toList());

        // intersected 정렬
        intersected.sort(
                (r1, r2) -> {
                    double diff1 = Math.abs(r1.getAvgTime1() - r1.getAvgTime2());
                    double diff2 = Math.abs(r2.getAvgTime1() - r2.getAvgTime2());

                    double score1 =
                            (diff1 > 30)
                                    ? 0
                                    : (0.25 * (1 - (diff1 / 30.0))
                                            + 0.35
                                                    * Math.max(
                                                            0,
                                                            (1
                                                                    - ((r1.getAvgTime1()
                                                                                    + r1
                                                                                            .getAvgTime2())
                                                                            / 120.0)))
                                            + 0.40
                                                    * Math.log10(
                                                            r1.getTotalMobility1()
                                                                    + r1.getTotalMobility2()));

                    double score2 =
                            (diff2 > 30)
                                    ? 0
                                    : (0.25 * (1 - (diff2 / 30.0))
                                            + 0.35
                                                    * Math.max(
                                                            0,
                                                            (1
                                                                    - ((r2.getAvgTime1()
                                                                                    + r2
                                                                                            .getAvgTime2())
                                                                            / 120.0)))
                                            + 0.40
                                                    * Math.log10(
                                                            r2.getTotalMobility1()
                                                                    + r2.getTotalMobility2()));

                    return Double.compare(score2, score1);
                });

        // 정렬
        Comparator<MobilityResponse> byMobilityDesc =
                Comparator.comparingDouble(MobilityResponse::getTotalMobility).reversed();
        firstMobility.sort(byMobilityDesc);
        secondMobility.sort(byMobilityDesc);

        List<MobilityResponse> intersectedResponses =
                intersected.stream().map(IntersectedMobilityResponse::toMobilityResponse).toList();

        MultiMobilityResponse result =
                MultiMobilityResponse.builder()
                        .firstMobility(first)
                        .secondMobility(second)
                        .intersectedMobility(listToPage(intersectedResponses, page))
                        .build();

        return ResponseDTO.res(HttpStatus.OK, "출발지 데이터 병합 완료", result);
    }

    @Deprecated
    public <T> List<T> listToPage(List<T> list, int page) {
        int totalElements = list.size();
        int fromIndex = page * 10;
        int toIndex = Math.min(fromIndex + 10, totalElements);

        if (fromIndex < totalElements) {
            return list.subList(fromIndex, toIndex);
        }
        return Collections.emptyList();
    }

    private double toDouble(Long value) {
        return value == null ? 0.0 : value.doubleValue();
    }

    private double avgJeonseDeposit(AdminDongRentPriceSummaryResponse rentPriceSummary) {
        if (rentPriceSummary == null || rentPriceSummary.jeonse() == null) {
            return 0.0;
        }
        return toDouble(rentPriceSummary.jeonse().amount());
    }

    private double avgMonthlyDeposit(AdminDongRentPriceSummaryResponse rentPriceSummary) {
        if (rentPriceSummary == null || rentPriceSummary.monthlyRent() == null) {
            return 0.0;
        }
        return toDouble(rentPriceSummary.monthlyRent().deposit());
    }

    private double avgMonthlyRent(AdminDongRentPriceSummaryResponse rentPriceSummary) {
        if (rentPriceSummary == null || rentPriceSummary.monthlyRent() == null) {
            return 0.0;
        }
        return toDouble(rentPriceSummary.monthlyRent().monthlyRent());
    }
}
