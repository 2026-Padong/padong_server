package com.example.padong_server.domain.activityMobility.service;

import com.example.padong_server.domain.activityMobility.dto.IntersectedMobilityResponse;
import com.example.padong_server.domain.activityMobility.dto.MobilityResponse;
import com.example.padong_server.domain.activityMobility.dto.MobilitySimpleResponse;
import com.example.padong_server.domain.activityMobility.dto.MultiMobilityResponse;
import com.example.padong_server.domain.activityMobility.entity.Mobility;
import com.example.padong_server.domain.activityMobility.repository.MobilityRepository;
import com.example.padong_server.domain.dongne.dto.AdminDongDto;
import com.example.padong_server.domain.dongne.entity.AdminDong;
import com.example.padong_server.domain.dongne.service.DongneService;
import com.example.padong_server.domain.population.service.PopulationService;
import com.example.padong_server.domain.rentPrice.dto.response.AdminDongRentPriceSummaryResponse;
import com.example.padong_server.domain.rentPrice.service.RentPriceService;
import com.example.padong_server.domain.score.service.ScoreCalculator;
import com.example.padong_server.global.ResponseDTO;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    private List<MobilitySimpleResponse> mapToSimpleResponses(Collection<Mobility> mobilities) {
        return mobilities.stream()
                .map(this::toSimpleResponse)
                .toList();
    }

    private MobilitySimpleResponse toSimpleResponse(Mobility mobility) {
        AdminDong departureDong = mobility.getDepartureDong();
        return MobilitySimpleResponse.builder()
                .departureDong(AdminDongDto.builder()
                        .adminDongCode(departureDong.getAdminDongCode())
                        .address(departureDong.getCityName()
                                + " "
                                + departureDong.getDistrictName()
                                + " "
                                + departureDong.getAdminDongName())
                        .build())
                .totalMobility(Math.round(mobility.getTotalMobility() * 100) / 100.0)
                .avgTime(Math.round(mobility.getAvgTime() * 100) / 100.0)
                .build();
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
