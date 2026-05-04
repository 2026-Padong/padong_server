package com.example.padong_server.domain.hotplace.service;

import com.example.padong_server.domain.hotplace.dto.AgeDetail;
import com.example.padong_server.domain.hotplace.dto.AgeDistributionItem;
import com.example.padong_server.domain.hotplace.dto.DistrictRealtimeResponse;
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
import com.example.padong_server.global.client.seoul.SeoulRealtimeClient;
import com.example.padong_server.global.client.seoul.SeoulRealtimeData;
import com.example.padong_server.global.exception.CustomException;
import com.example.padong_server.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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
    private final SeoulRealtimeClient seoulRealtimeClient;
    private final SubwayTransferInfoService subwayTransferInfoService;

    public DistrictRealtimeResponse getDistrictRealtime(String guName) {
        validateGuName(guName);

        List<HotPlace> hotPlaces = hotPlaceRepository.findByGuName(guName);
        if (hotPlaces.isEmpty()) {
            throw new CustomException(ErrorCode.HOTPLACE_NOT_FOUND);
        }

        HotPlace selectedHotPlace = hotPlaces.get(0);
        Map<String, SeoulRealtimeData> realtimeCache = new HashMap<>();

        SeoulRealtimeData selectedRealtimeData = getRealtimeData(selectedHotPlace.getAreaNm(), realtimeCache);
        List<HotplaceRealtimeItem> hotplaceItems = hotPlaces.stream()
                .map(hotPlace -> toHotplaceRealtimeItem(hotPlace, getRealtimeData(hotPlace.getAreaNm(), realtimeCache)))
                .toList();

        return DistrictRealtimeResponse.builder()
                .guName(guName)
                .selectedAreaNm(selectedHotPlace.getAreaNm())
                .summary(toWeatherSummary(selectedRealtimeData))
                .hotplaces(hotplaceItems)
                .build();
    }

    private SeoulRealtimeData getRealtimeData(String areaNm, Map<String, SeoulRealtimeData> realtimeCache) {
        return realtimeCache.computeIfAbsent(areaNm, seoulRealtimeClient::getRealtimeDataByAreaNm);
    }

    private WeatherSummary toWeatherSummary(SeoulRealtimeData realtimeData) {
        return WeatherSummary.builder()
                .weatherStatus(defaultText(realtimeData.getWeatherStatus()))
                .temperature(formatDecimal(realtimeData.getTemperature()))
                .sensibleTemperature(formatDecimal(realtimeData.getSensibleTemperature()))
                .humidity(formatRoundedPercent(realtimeData.getHumidity()))
                .fineDustStatus(defaultText(realtimeData.getPm10Status()))
                .fineDust(formatDecimal(realtimeData.getPm10()))
                .precipitationProbability(formatRoundedPercent(realtimeData.getRainChance()))
                .build();
    }

    private HotplaceRealtimeItem toHotplaceRealtimeItem(HotPlace hotPlace, SeoulRealtimeData realtimeData) {
        return HotplaceRealtimeItem.builder()
                .areaNm(hotPlace.getAreaNm())
                .thumbnail(resolveThumbnail(hotPlace, realtimeData))
                .category(resolveCategory(hotPlace))
                .roadAddr(defaultText(realtimeData.getRoadAddr()))
                .eventNm(defaultText(realtimeData.getEventNm()))
                .weather(toWeatherSummary(realtimeData))
                .population(toPopulation(realtimeData))
                .age(toAge(realtimeData))
                .gender(toGender(realtimeData))
                .transport(toTransport(realtimeData))
                .roadTraffic(toRoadTraffic(realtimeData))
                .build();
    }

    private String resolveThumbnail(HotPlace hotPlace, SeoulRealtimeData realtimeData) {
        if (StringUtils.hasText(realtimeData.getThumbnail())) {
            return realtimeData.getThumbnail();
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
                .min(formatWholeNumber(realtimeData.getAreaPpltnMin()))
                .max(formatWholeNumber(realtimeData.getAreaPpltnMax()))
                .display(formatPopulationDisplay(realtimeData.getAreaPpltnMin(), realtimeData.getAreaPpltnMax()))
                .congestionLevel(defaultText(realtimeData.getAreaCongestLvl()))
                .congestionMessage(defaultText(realtimeData.getAreaCongestMsg()))
                .forecast(toPopulationForecast(realtimeData))
                .build();
    }

    private PopulationForecast toPopulationForecast(SeoulRealtimeData realtimeData) {
        return PopulationForecast.builder()
                .status(resolveForecastStatus(realtimeData))
                .populationMin(formatWholeNumber(realtimeData.getFcstPpltnMin()))
                .populationMax(formatWholeNumber(realtimeData.getFcstPpltnMax()))
                .time(defaultText(realtimeData.getFcstTime()))
                .build();
    }

    private String resolveForecastStatus(SeoulRealtimeData realtimeData) {
        if (!"Y".equalsIgnoreCase(realtimeData.getFcstYn())) {
            return DEFAULT_FORECAST_STATUS;
        }

        Double currentMid = midpoint(realtimeData.getAreaPpltnMin(), realtimeData.getAreaPpltnMax());
        Double forecastMid = midpoint(realtimeData.getFcstPpltnMin(), realtimeData.getFcstPpltnMax());
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
                        .rate10(formatPercent(realtimeData.getPpltnRate10()))
                        .rate20(formatPercent(realtimeData.getPpltnRate20()))
                        .rate30(formatPercent(realtimeData.getPpltnRate30()))
                        .rate40(formatPercent(realtimeData.getPpltnRate40()))
                        .rate50(formatPercent(realtimeData.getPpltnRate50()))
                        .rate60(formatPercent(realtimeData.getPpltnRate60()))
                        .rate70(formatPercent(realtimeData.getPpltnRate70()))
                        .build())
                .build();
    }

    private List<AgeDistributionItem> ageRanking(SeoulRealtimeData realtimeData) {
        Map<String, Double> ageRates = new LinkedHashMap<>();
        ageRates.put("10대", realtimeData.getPpltnRate10());
        ageRates.put("20대", realtimeData.getPpltnRate20());
        ageRates.put("30대", realtimeData.getPpltnRate30());
        ageRates.put("40대", realtimeData.getPpltnRate40());
        ageRates.put("50대", realtimeData.getPpltnRate50());
        ageRates.put("60대", realtimeData.getPpltnRate60());
        ageRates.put("70대", realtimeData.getPpltnRate70());

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
                .maleRate(formatPercent(realtimeData.getMalePpltnRate()))
                .femaleRate(formatPercent(realtimeData.getFemalePpltnRate()))
                .build();
    }

    private HotplaceTransport toTransport(SeoulRealtimeData realtimeData) {
        List<String> subwayStations = safeList(realtimeData.getSubwayStationNames());
        String primaryStation = subwayStations.isEmpty() ? DEFAULT_TEXT : subwayStations.get(0);
        List<String> subwayLines = subwayTransferInfoService.findLinesByStationName(primaryStation);
        if (subwayLines.isEmpty()) {
            subwayLines = safeList(realtimeData.getSubwayLines());
        }

        return HotplaceTransport.builder()
                .subway(SubwayTransportInfo.builder()
                        .stationName(primaryStation)
                        .lines(subwayLines)
                        .stationCount(subwayStations.size())
                        .build())
                .bus(NamedCountInfo.builder()
                        .count(safeList(realtimeData.getBusStopNames()).size())
                        .names(safeList(realtimeData.getBusStopNames()))
                        .build())
                .bike(NamedCountInfo.builder()
                        .count(safeList(realtimeData.getBikeStationNames()).size())
                        .names(safeList(realtimeData.getBikeStationNames()))
                        .build())
                .build();
    }

    private RoadTrafficInfo toRoadTraffic(SeoulRealtimeData realtimeData) {
        return RoadTrafficInfo.builder()
                .status(defaultText(realtimeData.getRoadTrafficIdx()))
                .speed(formatSpeed(realtimeData.getRoadTrafficSpd()))
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
