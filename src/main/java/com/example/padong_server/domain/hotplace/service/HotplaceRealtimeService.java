package com.example.padong_server.domain.hotplace.service;

import com.example.padong_server.domain.hotplace.dto.DistrictRealtimeResponse;
import com.example.padong_server.domain.hotplace.dto.HotplaceRealtimeItem;
import com.example.padong_server.domain.hotplace.dto.WeatherSummary;
import com.example.padong_server.domain.hotplace.entity.HotPlace;
import com.example.padong_server.domain.hotplace.repository.HotPlaceRepository;
import com.example.padong_server.global.client.seoul.SeoulRealtimeClient;
import com.example.padong_server.global.client.seoul.SeoulRealtimeData;
import com.example.padong_server.global.exception.CustomException;
import com.example.padong_server.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
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

    private static final String DEFAULT_CONGESTION_LEVEL = "\uC815\uBCF4 \uC5C6\uC74C";
    private static final String DEFAULT_CONGESTION_MESSAGE = "\uC815\uBCF4 \uC5C6\uC74C";
    private static final String DEFAULT_WEATHER_STATUS = "\uC815\uBCF4 \uC5C6\uC74C";
    private static final String DEFAULT_NUMERIC_TEXT = "\uC815\uBCF4 \uC5C6\uC74C";
    private static final Set<String> VALID_GU_NAMES = new HashSet<>(Arrays.asList(
            "\uAC15\uB0A8\uAD6C", "\uAC15\uB3D9\uAD6C", "\uAC15\uBD81\uAD6C", "\uAC15\uC11C\uAD6C", "\uAD00\uC545\uAD6C",
            "\uAD11\uC9C4\uAD6C", "\uAD6C\uB85C\uAD6C", "\uAE08\uCC9C\uAD6C", "\uB178\uC6D0\uAD6C", "\uB3C4\uBD09\uAD6C",
            "\uB3D9\uB300\uBB38\uAD6C", "\uB3D9\uC791\uAD6C", "\uB9C8\uD3EC\uAD6C", "\uC11C\uB300\uBB38\uAD6C", "\uC11C\uCD08\uAD6C",
            "\uC131\uB3D9\uAD6C", "\uC131\uBD81\uAD6C", "\uC1A1\uD30C\uAD6C", "\uC591\uCC9C\uAD6C", "\uC601\uB4F1\uD3EC\uAD6C",
            "\uC6A9\uC0B0\uAD6C", "\uC740\uD3C9\uAD6C", "\uC885\uB85C\uAD6C", "\uC911\uAD6C", "\uC911\uB791\uAD6C"
    ));

    private final HotPlaceRepository hotPlaceRepository;
    private final SeoulRealtimeClient seoulRealtimeClient;

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
        // TODO: Replace local request-scope caching with Redis caching or parallel API calls when hotplace count grows.
        return realtimeCache.computeIfAbsent(areaNm, seoulRealtimeClient::getRealtimeDataByAreaNm);
    }

    private WeatherSummary toWeatherSummary(SeoulRealtimeData realtimeData) {
        return WeatherSummary.builder()
                .weatherStatus(defaultText(realtimeData.getWeatherStatus(), DEFAULT_WEATHER_STATUS))
                .temperature(formatTemperature(realtimeData.getTemperature()))
                .sensibleTemperature(formatTemperature(realtimeData.getSensibleTemperature()))
                .humidity(formatHumidity(realtimeData.getHumidity()))
                .pm10Status(defaultText(realtimeData.getPm10Status(), DEFAULT_NUMERIC_TEXT))
                .pm10(formatPm10Value(realtimeData.getPm10()))
                .precipitationProbability(formatPrecipitationProbability(realtimeData.getRainChance()))
                .build();
    }

    private HotplaceRealtimeItem toHotplaceRealtimeItem(HotPlace hotPlace, SeoulRealtimeData realtimeData) {
        AgePeak agePeak = resolveDominantAge(realtimeData);

        return HotplaceRealtimeItem.builder()
                .areaNm(hotPlace.getAreaNm())
                .thumbnail(realtimeData.getThumbnail())
                .roadAddr(defaultText(realtimeData.getRoadAddr(), DEFAULT_NUMERIC_TEXT))
                .areaPpltnMin(formatWholeNumber(realtimeData.getAreaPpltnMin()))
                .areaPpltnMax(formatWholeNumber(realtimeData.getAreaPpltnMax()))
                .congestionLevel(defaultText(realtimeData.getAreaCongestLvl(), DEFAULT_CONGESTION_LEVEL))
                .dominantAgeGroup(agePeak.ageGroup())
                .dominantAgeRate(agePeak.rate())
                .roadTrafficIdx(defaultText(realtimeData.getRoadTrafficIdx(), DEFAULT_NUMERIC_TEXT))
                .roadTrafficSpd(formatSpeed(realtimeData.getRoadTrafficSpd()))
                .build();
    }

    private String formatTemperature(Double temperature) {
        if (temperature == null) {
            return DEFAULT_NUMERIC_TEXT;
        }
        return String.format(Locale.US, "%.1f", temperature);
    }

    private String formatPm10Value(Double pm10) {
        if (pm10 == null) {
            return DEFAULT_NUMERIC_TEXT;
        }
        return String.format(Locale.US, "%.1f", pm10);
    }

    private String formatPrecipitationProbability(Double rainChance) {
        if (rainChance == null) {
            return DEFAULT_NUMERIC_TEXT;
        }
        int value = (int) Math.round(rainChance);
        return value + "%";
    }

    private String formatHumidity(Double humidity) {
        if (humidity == null) {
            return DEFAULT_NUMERIC_TEXT;
        }
        int value = (int) Math.round(humidity);
        return value + "%";
    }

    private String formatWholeNumber(Double value) {
        if (value == null) {
            return DEFAULT_NUMERIC_TEXT;
        }
        return String.valueOf((int) Math.round(value));
    }

    private String formatPercent(Double value) {
        if (value == null) {
            return DEFAULT_NUMERIC_TEXT;
        }
        return String.format(Locale.US, "%.1f%%", value);
    }

    private String formatSpeed(Double speed) {
        if (speed == null) {
            return DEFAULT_NUMERIC_TEXT;
        }
        return String.format(Locale.US, "%.1f", speed);
    }

    private AgePeak resolveDominantAge(SeoulRealtimeData realtimeData) {
        Map<String, Double> ageRates = new LinkedHashMap<>();
        ageRates.put("10대", realtimeData.getPpltnRate10());
        ageRates.put("20대", realtimeData.getPpltnRate20());
        ageRates.put("30대", realtimeData.getPpltnRate30());
        ageRates.put("40대", realtimeData.getPpltnRate40());
        ageRates.put("50대", realtimeData.getPpltnRate50());
        ageRates.put("60대", realtimeData.getPpltnRate60());

        String dominantAgeGroup = DEFAULT_NUMERIC_TEXT;
        Double dominantAgeRate = null;

        for (Map.Entry<String, Double> entry : ageRates.entrySet()) {
            Double value = entry.getValue();
            if (value == null) {
                continue;
            }
            if (dominantAgeRate == null || value > dominantAgeRate) {
                dominantAgeGroup = entry.getKey();
                dominantAgeRate = value;
            }
        }

        return new AgePeak(dominantAgeGroup, formatPercent(dominantAgeRate));
    }

    private String defaultText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private void validateGuName(String guName) {
        if (!StringUtils.hasText(guName) || !VALID_GU_NAMES.contains(guName)) {
            throw new CustomException(ErrorCode.INVALID_GU_NAME);
        }
    }

    private record AgePeak(String ageGroup, String rate) {
    }
}
