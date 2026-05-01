package com.example.padong_server.domain.activityMobility.service;

import com.example.padong_server.domain.activityMobility.dto.CommonDepartureMobilityResponse;
import com.example.padong_server.domain.activityMobility.dto.IntersectedMobilityResponse;
import com.example.padong_server.domain.activityMobility.dto.MobilityFilterRequest;
import com.example.padong_server.domain.activityMobility.dto.MobilityResponse;
import com.example.padong_server.domain.activityMobility.dto.MobilitySimpleResponse;
import com.example.padong_server.domain.activityMobility.dto.MultiMobilityResponse;
import com.example.padong_server.domain.activityMobility.entity.Mobility;
import com.example.padong_server.domain.activityMobility.repository.MobilityRepository;
import com.example.padong_server.domain.dongne.dto.AdminDongDto;
import com.example.padong_server.domain.dongne.entity.AdminDong;
import com.example.padong_server.domain.dongne.service.DongneService;
import com.example.padong_server.domain.population.service.PopulationService;
import com.example.padong_server.domain.rentPrice.dto.response.AdminDongRentPriceBuildingTypeResponse;
import com.example.padong_server.domain.rentPrice.dto.response.AdminDongRentPriceDetailResponse;
import com.example.padong_server.domain.rentPrice.dto.response.AdminDongRentPriceSummaryResponse;
import com.example.padong_server.domain.rentPrice.dto.response.MonthlyRentDisplayValueResponse;
import com.example.padong_server.domain.rentPrice.dto.response.RentPriceDisplayValueResponse;
import com.example.padong_server.domain.rentPrice.entity.RentPriceTradeType;
import com.example.padong_server.domain.rentPrice.entity.ResidenceBuildingType;
import com.example.padong_server.domain.rentPrice.service.RentPriceService;
import com.example.padong_server.domain.score.service.ScoreCalculator;
import com.example.padong_server.global.ResponseDTO;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MobilityService {

    private final MobilityRepository mobilityRepository;
    private final DongneService dongneService;
    private final PopulationService populationService;
    private final RentPriceService rentPriceService;
    private final ScoreCalculator scoreCalculator;

    public List<MobilityResponse> mapFromEntities(Collection<Mobility> mobilities) {
        Map<String, AdminDongRentPriceSummaryResponse> rentPriceSummaryByAdminDongCode =
                rentPriceService.getSummaries(mobilities.stream()
                                .map(Mobility::getDepartureDong)
                                .map(AdminDong::getAdminDongCode)
                                .distinct()
                                .toList(), null, null)
                        .stream()
                        .collect(Collectors.toMap(
                                AdminDongRentPriceSummaryResponse::adminDongCode,
                                Function.identity(),
                                (a, b) -> a
                        ));

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

            responses.add(MobilityResponse.builder()
                    .departureDong(AdminDongDto.builder()
                            .adminDongCode(departureDong.getAdminDongCode())
                            .address(departureDong.getCityName() + " " + departureDong.getDistrictName() + " " + departureDong.getAdminDongName())
                            .build())
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

    public ResponseDTO<List<MobilityResponse>> searchByAddress(String address, Pageable pageable) {
        AdminDong adminDong = dongneService.findAdminDongByAddress(address);
        Page<Mobility> mobilities = mobilityRepository.findByArrivalDong(adminDong, pageable);
        if (mobilities.isEmpty())
            return ResponseDTO.res(HttpStatus.NOT_FOUND, "해당 행정동이 존재하지 않습니다.");

        return ResponseDTO.res(HttpStatus.OK, "출발 행정동 조회 성공", mapFromEntities(mobilities.getContent()));
    }

    public ResponseDTO<List<MobilitySimpleResponse>> searchByArrivalDongCode(
            String adminDongCode,
            Pageable pageable
    ) {
        AdminDong adminDong = dongneService.findAdminDongByCode(adminDongCode);
        Page<Mobility> mobilities = mobilityRepository.findByArrivalDong(adminDong, pageable);
        if (mobilities.isEmpty()) {
            return ResponseDTO.res(HttpStatus.NOT_FOUND, "해당 생활이동 데이터가 존재하지 않습니다.");
        }

        return ResponseDTO.res(HttpStatus.OK, "생활이동 많은 순 조회 성공", mapToSimpleResponses(mobilities.getContent()));
    }

    public ResponseDTO<List<MobilitySimpleResponse>> searchByArrivalDongCode(
            String adminDongCode,
            Pageable pageable,
            MobilityFilterRequest filterRequest
    ) {
        MobilityFilterRequest filter = filterOrEmpty(filterRequest);
        String validationError = validateFilter(filter);
        if (validationError != null) {
            return ResponseDTO.res(HttpStatus.BAD_REQUEST, validationError);
        }
        if (!hasSingleFilter(filter)) {
            return searchByArrivalDongCode(adminDongCode, pageable);
        }

        AdminDong adminDong = dongneService.findAdminDongByCode(adminDongCode);
        List<Mobility> mobilities = mobilityRepository.findByArrivalDong(adminDong);
        if (mobilities.isEmpty()) {
            return ResponseDTO.res(HttpStatus.NOT_FOUND, "해당 생활이동 데이터가 존재하지 않습니다.");
        }

        List<Mobility> filteredMobilities = filterSingleMobilities(mobilities, filter);
        if (filteredMobilities.isEmpty()) {
            return ResponseDTO.res(HttpStatus.NOT_FOUND, "조건에 맞는 생활이동 데이터가 존재하지 않습니다.");
        }

        return ResponseDTO.res(
                HttpStatus.OK,
                "생활이동 많은 순 조회 성공",
                mapToSimpleResponses(listToPage(filteredMobilities, pageable))
        );
    }

    public ResponseDTO<List<CommonDepartureMobilityResponse>> searchByArrivalDongCodes(
            List<String> adminDongCodes,
            Pageable pageable
    ) {
        return searchByArrivalDongCodes(adminDongCodes, pageable, MobilityFilterRequest.empty());
    }

    public ResponseDTO<List<CommonDepartureMobilityResponse>> searchByArrivalDongCodes(
            List<String> adminDongCodes,
            Pageable pageable,
            MobilityFilterRequest filterRequest
    ) {
        MobilityFilterRequest filter = filterOrEmpty(filterRequest);
        String validationError = validateFilter(filter);
        if (validationError != null) {
            return ResponseDTO.res(HttpStatus.BAD_REQUEST, validationError);
        }

        List<String> distinctAdminDongCodes = normalizeAdminDongCodes(adminDongCodes);
        if (distinctAdminDongCodes.size() < 2) {
            return ResponseDTO.res(HttpStatus.BAD_REQUEST, "행정동 코드는 2개 이상 입력해야 합니다.");
        }

        List<AdminDong> arrivalDongs = distinctAdminDongCodes.stream()
                .map(dongneService::findAdminDongByCode)
                .toList();
        List<Mobility> mobilities = mobilityRepository.findByArrivalDongIn(arrivalDongs);
        List<CommonDepartureMobilityResponse> responses = mapToCommonDepartureResponses(
                mobilities,
                distinctAdminDongCodes.size(),
                pageable,
                filter
        );
        if (responses.isEmpty()) {
            return ResponseDTO.res(HttpStatus.NOT_FOUND, "공통 생활이동 데이터가 존재하지 않습니다.");
        }

        return ResponseDTO.res(HttpStatus.OK, "다중 행정동 생활이동 많은 순 조회 성공", responses);
    }

    public Mobility findByGeoCodes(String arrivalCode, String departureCode){
        AdminDong arrivalDong= dongneService.findAdminDongByCode(arrivalCode);
        AdminDong departureDong= dongneService.findAdminDongByCode(departureCode);
        Optional<Mobility>optional= mobilityRepository.findByArrivalDongAndDepartureDong(arrivalDong,departureDong);
      return optional.orElse(null);

    }

    public ResponseDTO<MultiMobilityResponse> searchByTwoAddresses(String address1, String address2, int page) {
        AdminDong adminDong1 = dongneService.findAdminDongByAddress(address1);
        AdminDong adminDong2 = dongneService.findAdminDongByAddress(address2);

        List<MobilityResponse> firstMobility = mapFromEntities(mobilityRepository.findByArrivalDong(adminDong1));
        List<MobilityResponse> secondMobility = mapFromEntities(mobilityRepository.findByArrivalDong(adminDong2));

        Pageable pageable = PageRequest.of(page, 10, Sort.by(Sort.Direction.DESC, "totalMobility"));
        List<MobilityResponse> first = searchByAddress(address1, pageable).getData();
        List<MobilityResponse> second = searchByAddress(address2, pageable).getData();

        Map<String, MobilityResponse> map1 = firstMobility.stream()
                .collect(Collectors.toMap(
                        r -> r.getDepartureDong().getAdminDongCode(),
                        Function.identity(),
                        (a, b) -> a
                ));

        Map<String, MobilityResponse> map2 = secondMobility.stream()
                .collect(Collectors.toMap(
                        r -> r.getDepartureDong().getAdminDongCode(),
                        Function.identity(),
                        (a, b) -> a
                ));

        List<IntersectedMobilityResponse> intersected = new ArrayList<>();

        for (String code : map1.keySet()) {
            if (map2.containsKey(code)) {
                MobilityResponse r1 = map1.get(code);
                MobilityResponse r2 = map2.get(code);

                intersected.add(IntersectedMobilityResponse.builder()
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
        intersected = intersected.stream()
                .filter(r ->
                        r.getTotalMobility1() >= 10 &&
                                r.getTotalMobility2() >= 10 &&
                                (r.getTotalMobility1() + r.getTotalMobility2() >= 50) &&
                                r.getAvgTime1() >= 20 &&
                                r.getAvgTime2() >= 20
                )
                .collect(Collectors.toList());

        // intersected 정렬
        intersected.sort((r1, r2) -> {
            double diff1 = Math.abs(r1.getAvgTime1() - r1.getAvgTime2());
            double diff2 = Math.abs(r2.getAvgTime1() - r2.getAvgTime2());

            double score1 = (diff1 > 30) ? 0 : (
                    0.25 * (1 - (diff1 / 30.0)) +
                            0.35 * Math.max(0, (1 - ((r1.getAvgTime1() + r1.getAvgTime2()) / 120.0))) +
                            0.40 * Math.log10(r1.getTotalMobility1() + r1.getTotalMobility2())
            );

            double score2 = (diff2 > 30) ? 0 : (
                    0.25 * (1 - (diff2 / 30.0)) +
                            0.35 * Math.max(0, (1 - ((r2.getAvgTime1() + r2.getAvgTime2()) / 120.0))) +
                            0.40 * Math.log10(r2.getTotalMobility1() + r2.getTotalMobility2())
            );

            return Double.compare(score2, score1);
        });

        // 정렬
        Comparator<MobilityResponse> byMobilityDesc = Comparator.comparingDouble(MobilityResponse::getTotalMobility).reversed();
        firstMobility.sort(byMobilityDesc);
        secondMobility.sort(byMobilityDesc);

        List<MobilityResponse> intersectedResponses = intersected.stream()
                .map(IntersectedMobilityResponse::toMobilityResponse)
                .toList();

        MultiMobilityResponse result = MultiMobilityResponse.builder()
                .firstMobility(first)
                .secondMobility(second)
                .intersectedMobility(listToPage(intersectedResponses, page))
                .build();

        return ResponseDTO.res(HttpStatus.OK, "출발지 데이터 병합 완료", result);
    }

    public <T> List<T> listToPage(List<T> list, int page) {
        int totalElements = list.size();
        int fromIndex = page * 10;
        int toIndex = Math.min(fromIndex + 10, totalElements);

        if (fromIndex < totalElements) {
            return list.subList(fromIndex, toIndex);
        }
        return Collections.emptyList();
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

    private MobilityFilterRequest filterOrEmpty(MobilityFilterRequest filterRequest) {
        return filterRequest == null ? MobilityFilterRequest.empty() : filterRequest;
    }

    private String validateFilter(MobilityFilterRequest filter) {
        String rangeValidationError = validateRanges(filter);
        if (rangeValidationError != null) {
            return rangeValidationError;
        }

        String contractType = trimToNull(filter.getContractType());
        String houseType = trimToNull(filter.getHouseType());
        if (contractType != null) {
            try {
                RentPriceTradeType.from(contractType);
            } catch (IllegalArgumentException exception) {
                return "지원하지 않는 거래 형태입니다: " + contractType;
            }
        }
        if (houseType != null) {
            try {
                ResidenceBuildingType.from(houseType);
            } catch (IllegalArgumentException exception) {
                return "지원하지 않는 집 형태입니다: " + houseType;
            }
        }

        boolean hasSalePriceFilter = hasRange(filter.getMinSalePrice(), filter.getMaxSalePrice());
        boolean hasJeonsePriceFilter = hasRange(filter.getMinJeonseDeposit(), filter.getMaxJeonseDeposit());
        boolean hasMonthlyPriceFilter = hasRange(filter.getMinMonthlyDeposit(), filter.getMaxMonthlyDeposit())
                || hasRange(filter.getMinMonthlyRent(), filter.getMaxMonthlyRent());
        if ((hasSalePriceFilter || hasJeonsePriceFilter || hasMonthlyPriceFilter) && contractType == null) {
            return "가격 필터를 사용하려면 거래 형태를 선택해야 합니다.";
        }

        if (contractType == null) {
            return null;
        }
        RentPriceTradeType tradeType = RentPriceTradeType.from(contractType);
        if (tradeType == RentPriceTradeType.SALE && (hasJeonsePriceFilter || hasMonthlyPriceFilter)) {
            return "매매 필터에는 전세/월세 가격 필터를 함께 사용할 수 없습니다.";
        }
        if (tradeType == RentPriceTradeType.JEONSE && (hasSalePriceFilter || hasMonthlyPriceFilter)) {
            return "전세 필터에는 매매/월세 가격 필터를 함께 사용할 수 없습니다.";
        }
        if (tradeType == RentPriceTradeType.MONTHLY_RENT && (hasSalePriceFilter || hasJeonsePriceFilter)) {
            return "월세 필터에는 매매/전세 가격 필터를 함께 사용할 수 없습니다.";
        }
        return null;
    }

    private String validateRanges(MobilityFilterRequest filter) {
        String invalidRangeName = firstInvalidRangeName(filter);
        if (invalidRangeName != null) {
            return "필터 범위의 최소값은 최대값보다 클 수 없습니다: " + invalidRangeName;
        }
        if (hasNegative(filter.getMinAvgTime(), filter.getMaxAvgTime(),
                filter.getMinEachAvgTime(), filter.getMaxEachAvgTime())) {
            return "시간 범위 값은 0 이상이어야 합니다.";
        }
        if (hasNegative(filter.getMinSalePrice(), filter.getMaxSalePrice(),
                filter.getMinJeonseDeposit(), filter.getMaxJeonseDeposit(),
                filter.getMinMonthlyDeposit(), filter.getMaxMonthlyDeposit(),
                filter.getMinMonthlyRent(), filter.getMaxMonthlyRent())) {
            return "가격 범위 값은 0 이상이어야 합니다.";
        }
        return null;
    }

    private String firstInvalidRangeName(MobilityFilterRequest filter) {
        if (isInvalidRange(filter.getMinAvgTime(), filter.getMaxAvgTime())) {
            return "minAvgTime/maxAvgTime";
        }
        if (isInvalidRange(filter.getMinEachAvgTime(), filter.getMaxEachAvgTime())) {
            return "minEachAvgTime/maxEachAvgTime";
        }
        if (isInvalidRange(filter.getMinSalePrice(), filter.getMaxSalePrice())) {
            return "minSalePrice/maxSalePrice";
        }
        if (isInvalidRange(filter.getMinJeonseDeposit(), filter.getMaxJeonseDeposit())) {
            return "minJeonseDeposit/maxJeonseDeposit";
        }
        if (isInvalidRange(filter.getMinMonthlyDeposit(), filter.getMaxMonthlyDeposit())) {
            return "minMonthlyDeposit/maxMonthlyDeposit";
        }
        if (isInvalidRange(filter.getMinMonthlyRent(), filter.getMaxMonthlyRent())) {
            return "minMonthlyRent/maxMonthlyRent";
        }
        return null;
    }

    private boolean hasSingleFilter(MobilityFilterRequest filter) {
        return hasRange(filter.getMinAvgTime(), filter.getMaxAvgTime())
                || !normalizeDistrictNames(filter.getDepartureDistrictNames()).isEmpty()
                || hasRentPriceFilter(filter);
    }

    private boolean hasRentPriceFilter(MobilityFilterRequest filter) {
        return trimToNull(filter.getContractType()) != null
                || trimToNull(filter.getHouseType()) != null
                || hasRange(filter.getMinSalePrice(), filter.getMaxSalePrice())
                || hasRange(filter.getMinJeonseDeposit(), filter.getMaxJeonseDeposit())
                || hasRange(filter.getMinMonthlyDeposit(), filter.getMaxMonthlyDeposit())
                || hasRange(filter.getMinMonthlyRent(), filter.getMaxMonthlyRent());
    }

    private List<Mobility> filterSingleMobilities(
            List<Mobility> mobilities,
            MobilityFilterRequest filter
    ) {
        Set<String> districtNames = normalizeDistrictNames(filter.getDepartureDistrictNames());
        List<Mobility> filteredMobilities = mobilities.stream()
                .filter(mobility -> matchesRange(mobility.getAvgTime(), filter.getMinAvgTime(), filter.getMaxAvgTime()))
                .filter(mobility -> districtNames.isEmpty()
                        || districtNames.contains(mobility.getDepartureDong().getDistrictName()))
                .sorted(Comparator
                        .comparingDouble(Mobility::getTotalMobility)
                        .reversed()
                        .thenComparing(mobility -> mobility.getDepartureDong().getAdminDongCode()))
                .toList();
        return filterMobilitiesByRentPrice(filteredMobilities, filter);
    }

    private List<Mobility> filterMultiMobilities(
            List<Mobility> mobilities,
            MobilityFilterRequest filter
    ) {
        Set<String> districtNames = normalizeDistrictNames(filter.getDepartureDistrictNames());
        return mobilities.stream()
                .filter(mobility -> matchesRange(
                        mobility.getAvgTime(),
                        filter.getMinEachAvgTime(),
                        filter.getMaxEachAvgTime()
                ))
                .filter(mobility -> districtNames.isEmpty()
                        || districtNames.contains(mobility.getDepartureDong().getDistrictName()))
                .toList();
    }

    private List<Mobility> filterMobilitiesByRentPrice(
            List<Mobility> mobilities,
            MobilityFilterRequest filter
    ) {
        if (!hasRentPriceFilter(filter) || mobilities.isEmpty()) {
            return mobilities;
        }
        Set<String> matchedAdminDongCodes = findRentPriceMatchedAdminDongCodes(
                mobilities.stream()
                        .map(Mobility::getDepartureDong)
                        .toList(),
                filter
        );
        return mobilities.stream()
                .filter(mobility -> matchedAdminDongCodes.contains(mobility.getDepartureDong().getAdminDongCode()))
                .toList();
    }

    private List<MultiArrivalAccumulator> filterMultiAccumulatorsByRentPrice(
            List<MultiArrivalAccumulator> accumulators,
            MobilityFilterRequest filter
    ) {
        if (!hasRentPriceFilter(filter) || accumulators.isEmpty()) {
            return accumulators;
        }
        Set<String> matchedAdminDongCodes = findRentPriceMatchedAdminDongCodes(
                accumulators.stream()
                        .map(MultiArrivalAccumulator::departureDong)
                        .toList(),
                filter
        );
        return accumulators.stream()
                .filter(accumulator -> matchedAdminDongCodes.contains(
                        accumulator.departureDong().getAdminDongCode()
                ))
                .toList();
    }

    private Set<String> findRentPriceMatchedAdminDongCodes(
            Collection<AdminDong> departureDongs,
            MobilityFilterRequest filter
    ) {
        List<String> adminDongCodes = departureDongs.stream()
                .map(AdminDong::getAdminDongCode)
                .distinct()
                .toList();
        Map<String, AdminDongRentPriceDetailResponse> rentPriceDetailByAdminDongCode =
                rentPriceService.getDetails(adminDongCodes)
                        .stream()
                        .collect(Collectors.toMap(
                                AdminDongRentPriceDetailResponse::adminDongCode,
                                Function.identity(),
                                (a, b) -> a
                        ));

        Set<String> matchedAdminDongCodes = new LinkedHashSet<>();
        for (String adminDongCode : adminDongCodes) {
            AdminDongRentPriceDetailResponse detail = rentPriceDetailByAdminDongCode.get(adminDongCode);
            if (matchesRentPrice(detail, filter)) {
                matchedAdminDongCodes.add(adminDongCode);
            }
        }
        return matchedAdminDongCodes;
    }

    private boolean matchesRentPrice(
            AdminDongRentPriceDetailResponse detail,
            MobilityFilterRequest filter
    ) {
        if (detail == null || detail.buildingTypes() == null) {
            return false;
        }
        RentPriceTradeType contractType = parseContractTypeOrNull(filter.getContractType());
        ResidenceBuildingType houseType = parseHouseTypeOrNull(filter.getHouseType());
        return detail.buildingTypes().stream()
                .filter(response -> matchesHouseType(response, houseType))
                .anyMatch(response -> matchesContractTypeAndPrice(response, contractType, filter));
    }

    private boolean matchesHouseType(
            AdminDongRentPriceBuildingTypeResponse response,
            ResidenceBuildingType houseType
    ) {
        if (houseType == null) {
            return true;
        }
        return response.buildingType() != null
                && houseType.code().equals(response.buildingType().buildingTypeCode());
    }

    private boolean matchesContractTypeAndPrice(
            AdminDongRentPriceBuildingTypeResponse response,
            RentPriceTradeType contractType,
            MobilityFilterRequest filter
    ) {
        if (contractType == null) {
            return hasAnyRentPrice(response);
        }
        return switch (contractType) {
            case SALE -> matchesSalePrice(response.sale(), filter);
            case JEONSE -> matchesJeonseDeposit(response.jeonse(), filter);
            case MONTHLY_RENT -> matchesMonthlyRent(response.monthlyRent(), filter);
        };
    }

    private boolean hasAnyRentPrice(AdminDongRentPriceBuildingTypeResponse response) {
        return response.sale() != null && response.sale().amount() != null
                || response.jeonse() != null && response.jeonse().amount() != null
                || response.monthlyRent() != null
                && response.monthlyRent().deposit() != null
                && response.monthlyRent().monthlyRent() != null;
    }

    private boolean matchesSalePrice(
            RentPriceDisplayValueResponse sale,
            MobilityFilterRequest filter
    ) {
        return sale != null
                && matchesMoneyRange(sale.amount(), filter.getMinSalePrice(), filter.getMaxSalePrice());
    }

    private boolean matchesJeonseDeposit(
            RentPriceDisplayValueResponse jeonse,
            MobilityFilterRequest filter
    ) {
        return jeonse != null
                && matchesMoneyRange(jeonse.amount(), filter.getMinJeonseDeposit(), filter.getMaxJeonseDeposit());
    }

    private boolean matchesMonthlyRent(
            MonthlyRentDisplayValueResponse monthlyRent,
            MobilityFilterRequest filter
    ) {
        return monthlyRent != null
                && matchesMoneyRange(
                        monthlyRent.deposit(),
                        filter.getMinMonthlyDeposit(),
                        filter.getMaxMonthlyDeposit()
                )
                && matchesMoneyRange(
                        monthlyRent.monthlyRent(),
                        filter.getMinMonthlyRent(),
                        filter.getMaxMonthlyRent()
                );
    }

    private boolean matchesMoneyRange(Long amountInManwon, Long minManwon, Long maxManwon) {
        if (amountInManwon == null) {
            return false;
        }
        if (minManwon != null && amountInManwon < minManwon) {
            return false;
        }
        return maxManwon == null || amountInManwon <= maxManwon;
    }

    private boolean matchesRange(double value, Double min, Double max) {
        if (min != null && value < min) {
            return false;
        }
        return max == null || value <= max;
    }

    private boolean hasRange(Double min, Double max) {
        return min != null || max != null;
    }

    private boolean hasRange(Long min, Long max) {
        return min != null || max != null;
    }

    private boolean isInvalidRange(Double min, Double max) {
        return min != null && max != null && min > max;
    }

    private boolean isInvalidRange(Long min, Long max) {
        return min != null && max != null && min > max;
    }

    private boolean hasNegative(Double... values) {
        for (Double value : values) {
            if (value != null && value < 0) {
                return true;
            }
        }
        return false;
    }

    private boolean hasNegative(Long... values) {
        for (Long value : values) {
            if (value != null && value < 0) {
                return true;
            }
        }
        return false;
    }

    private Set<String> normalizeDistrictNames(List<String> districtNames) {
        if (districtNames == null) {
            return Set.of();
        }
        return districtNames.stream()
                .map(this::trimToNull)
                .filter(name -> name != null)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private RentPriceTradeType parseContractTypeOrNull(String contractType) {
        String normalizedContractType = trimToNull(contractType);
        return normalizedContractType == null ? null : RentPriceTradeType.from(normalizedContractType);
    }

    private ResidenceBuildingType parseHouseTypeOrNull(String houseType) {
        String normalizedHouseType = trimToNull(houseType);
        return normalizedHouseType == null ? null : ResidenceBuildingType.from(normalizedHouseType);
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private List<MobilitySimpleResponse> mapToSimpleResponses(Collection<Mobility> mobilities) {
        return mobilities.stream()
                .map(this::toSimpleResponse)
                .toList();
    }

    private List<CommonDepartureMobilityResponse> mapToCommonDepartureResponses(
            List<Mobility> mobilities,
            int requiredArrivalDongCount,
            Pageable pageable,
            MobilityFilterRequest filter
    ) {
        Map<String, MultiArrivalAccumulator> accumulatorByDepartureDongCode = new LinkedHashMap<>();
        for (Mobility mobility : filterMultiMobilities(mobilities, filter)) {
            String departureDongCode = mobility.getDepartureDong().getAdminDongCode();
            accumulatorByDepartureDongCode.computeIfAbsent(
                            departureDongCode,
                            ignored -> new MultiArrivalAccumulator(mobility.getDepartureDong())
                    )
                    .add(mobility);
        }

        List<MultiArrivalAccumulator> accumulators = accumulatorByDepartureDongCode.values().stream()
                .filter(accumulator -> accumulator.arrivalDongCount() == requiredArrivalDongCount)
                .toList();
        accumulators = filterMultiAccumulatorsByRentPrice(accumulators, filter);

        List<CommonDepartureMobilityResponse> responses = accumulators.stream()
                .sorted(Comparator
                        .comparingDouble(MultiArrivalAccumulator::totalMobility)
                        .reversed()
                        .thenComparing(accumulator -> accumulator.departureDong().getAdminDongCode()))
                .map(this::toCommonDepartureResponse)
                .toList();
        return listToPage(responses, pageable);
    }

    private CommonDepartureMobilityResponse toCommonDepartureResponse(MultiArrivalAccumulator accumulator) {
        AdminDong departureDong = accumulator.departureDong();
        return CommonDepartureMobilityResponse.builder()
                .departureDong(toAdminDongDto(departureDong))
                .totalMobility(roundToSecondDecimal(accumulator.totalMobility()))
                .build();
    }

    private MobilitySimpleResponse toSimpleResponse(Mobility mobility) {
        AdminDong departureDong = mobility.getDepartureDong();
        return MobilitySimpleResponse.builder()
                .departureDong(toAdminDongDto(departureDong))
                .totalMobility(roundToSecondDecimal(mobility.getTotalMobility()))
                .avgTime(roundToSecondDecimal(mobility.getAvgTime()))
                .build();
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

    private AdminDongDto toAdminDongDto(AdminDong adminDong) {
        return AdminDongDto.builder()
                .adminDongCode(adminDong.getAdminDongCode())
                .address(adminDong.getCityName()
                        + " "
                        + adminDong.getDistrictName()
                        + " "
                        + adminDong.getAdminDongName())
                .build();
    }

    private double roundToSecondDecimal(double value) {
        return Math.round(value * 100) / 100.0;
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

    private static class MultiArrivalAccumulator {
        private final AdminDong departureDong;
        private final Set<String> arrivalDongCodes = new HashSet<>();
        private double totalMobility;

        private MultiArrivalAccumulator(AdminDong departureDong) {
            this.departureDong = departureDong;
        }

        private void add(Mobility mobility) {
            arrivalDongCodes.add(mobility.getArrivalDong().getAdminDongCode());
            totalMobility += mobility.getTotalMobility();
        }

        private AdminDong departureDong() {
            return departureDong;
        }

        private int arrivalDongCount() {
            return arrivalDongCodes.size();
        }

        private double totalMobility() {
            return totalMobility;
        }
    }
}
