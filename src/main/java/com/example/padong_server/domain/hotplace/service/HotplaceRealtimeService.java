package com.example.padong_server.domain.hotplace.service;

import com.example.padong_server.domain.hotplace.dto.AgeDetail;
import com.example.padong_server.domain.hotplace.dto.AgeDistributionItem;
import com.example.padong_server.domain.hotplace.dto.DistrictRealtimeResponse;
import com.example.padong_server.domain.hotplace.dto.DistrictSummaryResponse;
import com.example.padong_server.domain.hotplace.dto.HotplaceAge;
import com.example.padong_server.domain.hotplace.dto.HotplaceGender;
import com.example.padong_server.domain.hotplace.dto.HotplacePopulation;
import com.example.padong_server.domain.hotplace.dto.HotplaceRealtimeItem;
import com.example.padong_server.domain.hotplace.dto.HotplaceTransport;
import com.example.padong_server.domain.hotplace.dto.NamedCountInfo;
import com.example.padong_server.domain.hotplace.dto.PopulationForecast;
import com.example.padong_server.domain.hotplace.dto.RoadTrafficInfo;
import com.example.padong_server.domain.hotplace.dto.SubwayTransportInfo;
import com.example.padong_server.domain.hotplace.dto.WeatherSummary;
import com.example.padong_server.domain.hotplace.entity.HotPlace;
import com.example.padong_server.domain.hotplace.repository.HotPlaceRepository;
import com.example.padong_server.domain.subway.service.SubwayTransferInfoService;
import com.example.padong_server.global.client.seoul.SeoulRealtimeData;
import com.example.padong_server.global.exception.CustomException;
import com.example.padong_server.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
public class HotplaceRealtimeService {

    private static final String HOTPLACE_THUMBNAIL_BASE_URL = "https://data.seoul.go.kr/resources/img/guide/hotspot/";
    private static final String DEFAULT_TEXT = "정보 없음";
    private static final String DEFAULT_FORECAST_STATUS = "정보 없음";
    private static final DecimalFormat WHOLE_NUMBER_FORMAT = new DecimalFormat("#,###");
    private static final Set<String> VALID_GU_NAMES = new HashSet<>(Arrays.asList(
            "강남구", "강동구", "강북구", "강서구", "관악구",
            "광진구", "구로구", "금천구", "노원구", "도봉구",
            "동대문구", "동작구", "마포구", "서대문구", "서초구",
            "성동구", "성북구", "송파구", "양천구", "영등포구",
            "용산구", "은평구", "종로구", "중구", "중랑구"
    ));

    private final HotPlaceRepository hotPlaceRepository;
    private final RealtimeDataCache realtimeDataCache;
    private final SubwayTransferInfoService subwayTransferInfoService;

    /**
     * 자치구 날씨 요약. 대표 POI 1 개만 fetch — getDistrictRealtime 의 전체 POI fetch 우회.
     * 첫 POI 실패하면 다음 후보로 fallback. 모두 실패 시 마지막 예외 throw.
     */
    public DistrictSummaryResponse getDistrictSummary(String guName) {
        validateGuName(guName);
        List<HotPlace> hotPlaces = hotPlaceRepository.findByGuName(guName);
        if (hotPlaces.isEmpty()) {
            throw new CustomException(ErrorCode.HOTPLACE_NOT_FOUND);
        }

        CustomException lastFailure = null;
        for (HotPlace hp : hotPlaces) {
            try {
                SeoulRealtimeData data = realtimeDataCache.getOrFetch(hp.getAreaNm());
                return DistrictSummaryResponse.builder()
                        .guName(guName)
                        .selectedAreaNm(hp.getAreaNm())
                        .summary(toWeatherSummary(data))
                        .build();
            } catch (CustomException e) {
                lastFailure = e;
            }
        }
        throw lastFailure != null ? lastFailure : new CustomException(ErrorCode.HOTPLACE_NOT_FOUND);
    }

    /** 자치구 핫플레이스 전체 카드 리스트. cursor 페이징은 controller 가 slice. */
    public List<HotplaceRealtimeItem> getDistrictHotplaces(String guName) {
        return getDistrictRealtime(guName).getHotplaces();
    }

    /**
     * 자치구 핫플레이스 페이지 — 요청 페이지 안에 들어가는 POI 만 fetch.
     * 응답 페이로드: { content: List<Item>, totalElements: int }
     */
    public DistrictHotplacesPage getDistrictHotplacesPage(String guName, int page, int size) {
        validateGuName(guName);
        List<HotPlace> hotPlaces = hotPlaceRepository.findByGuName(guName);
        if (hotPlaces.isEmpty()) {
            throw new CustomException(ErrorCode.HOTPLACE_NOT_FOUND);
        }
        int total = hotPlaces.size();
        int from = Math.min(page * size, total);
        int to = Math.min(from + size, total);
        List<HotPlace> slice = hotPlaces.subList(from, to);

        List<HotplaceRealtimeItem> items = new ArrayList<>();
        for (HotPlace hp : slice) {
            try {
                SeoulRealtimeData data = realtimeDataCache.getOrFetch(hp.getAreaNm());
                items.add(toHotplaceRealtimeItem(hp, data));
            } catch (CustomException ignored) {
                // POI 1개 실패는 page 응답에서 skip — 전체 실패 막음
            }
        }
        return new DistrictHotplacesPage(items, total);
    }

    public record DistrictHotplacesPage(List<HotplaceRealtimeItem> content, int totalElements) {}

    /**
     * summary + hotplaces 를 한 번에 계산하는 내부 빌더. 외부 노출은 위의 두 분리 메서드를 통해.
     * 단, 서비스 단위 테스트가 이 메서드의 출력을 검증해 public 유지.
     */
    public DistrictRealtimeResponse getDistrictRealtime(String guName) {
        validateGuName(guName);

        List<HotPlace> hotPlaces = hotPlaceRepository.findByGuName(guName);
        if (hotPlaces.isEmpty()) {
            throw new CustomException(ErrorCode.HOTPLACE_NOT_FOUND);
        }

        // POI 별 외부 API 호출을 Reactor Flux 로 병렬 — 14개 직렬 ~3s → ~수백 ms.
        // - 먼저 areaNm distinct → 같은 자치구 안 중복 areaNm 은 1번만 호출
        // - flatMap: 병렬 실행 (순서 무관, 어차피 Map 으로 받음)
        // - subscribeOn(boundedElastic): blocking I/O 전용 reactor 스케줄러
        // - onErrorResume: POI 한 개 실패해도 다른 POI 응답은 살림
        AtomicReference<CustomException> lastFailure = new AtomicReference<>();
        List<String> uniqueAreaNms = hotPlaces.stream().map(HotPlace::getAreaNm).distinct().toList();
        Map<String, Optional<SeoulRealtimeData>> dataByAreaNm = Flux.fromIterable(uniqueAreaNms)
                .flatMap(areaNm ->
                        Mono.fromCallable(() -> realtimeDataCache.getOrFetch(areaNm))
                                .subscribeOn(Schedulers.boundedElastic())
                                .map(d -> Map.entry(areaNm, Optional.of(d)))
                                .onErrorResume(CustomException.class, e -> {
                                    lastFailure.set(e);
                                    return Mono.just(Map.entry(
                                            areaNm, Optional.<SeoulRealtimeData>empty()));
                                }))
                .collectMap(Map.Entry::getKey, Map.Entry::getValue)
                .block();

        // 입력 순서대로 결과 수집 — 첫 번째 성공 POI 가 자치구 대표
        List<HotplaceRealtimeItem> hotplaceItems = new ArrayList<>();
        HotPlace selectedHotPlace = null;
        SeoulRealtimeData selectedRealtimeData = null;
        for (HotPlace hp : hotPlaces) {
            Optional<SeoulRealtimeData> opt = dataByAreaNm.getOrDefault(hp.getAreaNm(), Optional.empty());
            if (opt.isEmpty()) {
                continue;
            }
            SeoulRealtimeData data = opt.get();
            if (selectedHotPlace == null) {
                selectedHotPlace = hp;
                selectedRealtimeData = data;
            }
            hotplaceItems.add(toHotplaceRealtimeItem(hp, data));
        }

        if (selectedHotPlace == null) {
            // 자치구 안 모든 POI 실패 — 한 예외 그대로 전파해 원인 노출
            throw lastFailure.get();
        }

        return DistrictRealtimeResponse.builder()
                .guName(guName)
                .selectedAreaNm(selectedHotPlace.getAreaNm())
                .summary(toWeatherSummary(selectedRealtimeData))
                .hotplaces(hotplaceItems)
                .build();
    }


    private WeatherSummary toWeatherSummary(SeoulRealtimeData realtimeData) {
        return WeatherSummary.builder()
                .weatherStatus(defaultText(realtimeData.weatherStatus()))
                .temperature(formatDecimal(realtimeData.temperature()))
                .sensibleTemperature(formatDecimal(realtimeData.sensibleTemperature()))
                .humidity(formatRoundedPercent(realtimeData.humidity()))
                .fineDustStatus(defaultText(realtimeData.pm10Status()))
                .fineDust(formatDecimal(realtimeData.pm10()))
                .precipitationProbability(formatRoundedPercent(realtimeData.rainChance()))
                .build();
    }

    private HotplaceRealtimeItem toHotplaceRealtimeItem(HotPlace hotPlace, SeoulRealtimeData realtimeData) {
        return HotplaceRealtimeItem.builder()
                .areaNm(hotPlace.getAreaNm())
                .thumbnail(resolveThumbnail(hotPlace, realtimeData))
                .category(resolveCategory(hotPlace))
                .roadAddr(defaultText(realtimeData.roadAddr()))
                .eventNm(defaultText(realtimeData.eventNm()))
                .weather(toWeatherSummary(realtimeData))
                .population(toPopulation(realtimeData))
                .age(toAge(realtimeData))
                .gender(toGender(realtimeData))
                .transport(toTransport(realtimeData))
                .roadTraffic(toRoadTraffic(realtimeData))
                .build();
    }

    private String resolveThumbnail(HotPlace hotPlace, SeoulRealtimeData realtimeData) {
        if (StringUtils.hasText(realtimeData.thumbnail())) {
            return realtimeData.thumbnail();
        }
        return HOTPLACE_THUMBNAIL_BASE_URL + hotPlace.getAreaNm() + ".jpg";
    }

    private String resolveCategory(HotPlace hotPlace) {
        if (hotPlace.getCategory() == null) {
            return DEFAULT_TEXT;
        }
        return hotPlace.getCategory().getDescription();
    }

    private HotplacePopulation toPopulation(SeoulRealtimeData realtimeData) {
        return HotplacePopulation.builder()
                .min(formatWholeNumber(realtimeData.areaPpltnMin()))
                .max(formatWholeNumber(realtimeData.areaPpltnMax()))
                .display(formatPopulationDisplay(realtimeData.areaPpltnMin(), realtimeData.areaPpltnMax()))
                .congestionLevel(defaultText(realtimeData.areaCongestLvl()))
                .congestionMessage(defaultText(realtimeData.areaCongestMsg()))
                .forecast(toPopulationForecast(realtimeData))
                .build();
    }

    private PopulationForecast toPopulationForecast(SeoulRealtimeData realtimeData) {
        return PopulationForecast.builder()
                .status(resolveForecastStatus(realtimeData))
                .populationMin(formatWholeNumber(realtimeData.fcstPpltnMin()))
                .populationMax(formatWholeNumber(realtimeData.fcstPpltnMax()))
                .time(defaultText(realtimeData.fcstTime()))
                .build();
    }

    private String resolveForecastStatus(SeoulRealtimeData realtimeData) {
        if (!"Y".equalsIgnoreCase(realtimeData.fcstYn())) {
            return DEFAULT_FORECAST_STATUS;
        }

        Double currentMid = midpoint(realtimeData.areaPpltnMin(), realtimeData.areaPpltnMax());
        Double forecastMid = midpoint(realtimeData.fcstPpltnMin(), realtimeData.fcstPpltnMax());
        if (currentMid == null || forecastMid == null) {
            return DEFAULT_FORECAST_STATUS;
        }

        if (forecastMid > currentMid) {
            return "증가 예상";
        }
        if (forecastMid < currentMid) {
            return "감소 예상";
        }
        return "변화 적음";
    }

    private HotplaceAge toAge(SeoulRealtimeData realtimeData) {
        List<AgeDistributionItem> ageRanking = ageRanking(realtimeData);
        AgeDistributionItem dominant = ageRanking.isEmpty()
                ? AgeDistributionItem.builder().ageGroup(DEFAULT_TEXT).rate(DEFAULT_TEXT).build()
                : ageRanking.get(0);

        return HotplaceAge.builder()
                .dominantGroup(dominant.getAgeGroup())
                .dominantRate(dominant.getRate())
                .top3(ageRanking.stream().limit(3).toList())
                .detail(AgeDetail.builder()
                        .rate10(formatPercent(realtimeData.ppltnRate10()))
                        .rate20(formatPercent(realtimeData.ppltnRate20()))
                        .rate30(formatPercent(realtimeData.ppltnRate30()))
                        .rate40(formatPercent(realtimeData.ppltnRate40()))
                        .rate50(formatPercent(realtimeData.ppltnRate50()))
                        .rate60(formatPercent(realtimeData.ppltnRate60()))
                        .rate70(formatPercent(realtimeData.ppltnRate70()))
                        .build())
                .build();
    }

    private List<AgeDistributionItem> ageRanking(SeoulRealtimeData realtimeData) {
        Map<String, Double> ageRates = new LinkedHashMap<>();
        ageRates.put("10대", realtimeData.ppltnRate10());
        ageRates.put("20대", realtimeData.ppltnRate20());
        ageRates.put("30대", realtimeData.ppltnRate30());
        ageRates.put("40대", realtimeData.ppltnRate40());
        ageRates.put("50대", realtimeData.ppltnRate50());
        ageRates.put("60대", realtimeData.ppltnRate60());
        ageRates.put("70대", realtimeData.ppltnRate70());

        return ageRates.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .sorted(Map.Entry.<String, Double>comparingByValue(Comparator.reverseOrder()))
                .map(entry -> AgeDistributionItem.builder()
                        .ageGroup(entry.getKey())
                        .rate(formatPercent(entry.getValue()))
                        .build())
                .toList();
    }

    private HotplaceGender toGender(SeoulRealtimeData realtimeData) {
        return HotplaceGender.builder()
                .maleRate(formatPercent(realtimeData.malePpltnRate()))
                .femaleRate(formatPercent(realtimeData.femalePpltnRate()))
                .build();
    }

    private HotplaceTransport toTransport(SeoulRealtimeData realtimeData) {
        List<String> subwayStations = safeList(realtimeData.subwayStationNames());
        String primaryStation = subwayStations.isEmpty() ? DEFAULT_TEXT : subwayStations.get(0);
        List<String> subwayLines = subwayTransferInfoService.findLinesByStationName(primaryStation);
        if (subwayLines.isEmpty()) {
            subwayLines = safeList(realtimeData.subwayLines());
        }

        return HotplaceTransport.builder()
                .subway(SubwayTransportInfo.builder()
                        .stationName(primaryStation)
                        .lines(subwayLines)
                        .stationCount(subwayStations.size())
                        .build())
                .bus(NamedCountInfo.builder()
                        .count(safeList(realtimeData.busStopNames()).size())
                        .names(safeList(realtimeData.busStopNames()))
                        .build())
                .bike(NamedCountInfo.builder()
                        .count(safeList(realtimeData.bikeStationNames()).size())
                        .names(safeList(realtimeData.bikeStationNames()))
                        .build())
                .build();
    }

    private RoadTrafficInfo toRoadTraffic(SeoulRealtimeData realtimeData) {
        return RoadTrafficInfo.builder()
                .status(defaultText(realtimeData.roadTrafficIdx()))
                .speed(formatSpeed(realtimeData.roadTrafficSpd()))
                .build();
    }

    private String formatPopulationDisplay(Double min, Double max) {
        Double midpoint = midpoint(min, max);
        if (midpoint == null) {
            return DEFAULT_TEXT;
        }
        return "약 " + WHOLE_NUMBER_FORMAT.format(Math.round(midpoint)) + "명";
    }

    private Double midpoint(Double min, Double max) {
        if (min == null && max == null) {
            return null;
        }
        if (min == null) {
            return max;
        }
        if (max == null) {
            return min;
        }
        return (min + max) / 2;
    }

    private String formatDecimal(Double value) {
        if (value == null) {
            return DEFAULT_TEXT;
        }
        return String.format(Locale.US, "%.1f", value);
    }

    private String formatPercent(Double value) {
        if (value == null) {
            return DEFAULT_TEXT;
        }
        return String.format(Locale.US, "%.1f%%", value);
    }

    private String formatRoundedPercent(Double value) {
        if (value == null) {
            return DEFAULT_TEXT;
        }
        return Math.round(value) + "%";
    }

    private String formatWholeNumber(Double value) {
        if (value == null) {
            return DEFAULT_TEXT;
        }
        return String.valueOf(Math.round(value));
    }

    private String formatSpeed(Double speed) {
        if (speed == null) {
            return DEFAULT_TEXT;
        }
        return Math.round(speed) + "km/h";
    }

    private String defaultText(String value) {
        return StringUtils.hasText(value) ? value : DEFAULT_TEXT;
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : values;
    }

    private void validateGuName(String guName) {
        if (!StringUtils.hasText(guName) || !VALID_GU_NAMES.contains(guName)) {
            throw new CustomException(ErrorCode.INVALID_GU_NAME);
        }
    }
}
