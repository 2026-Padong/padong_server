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
                .pm10(resolvePm10(realtimeData))
                .precipitationProbability(formatPrecipitationProbability(realtimeData.getRainChance()))
                .build();
    }

    private HotplaceRealtimeItem toHotplaceRealtimeItem(HotPlace hotPlace, SeoulRealtimeData realtimeData) {
        return HotplaceRealtimeItem.builder()
                .category(hotPlace.getCategory().getDescription())
                .areaNm(hotPlace.getAreaNm())
                .congestionLevel(defaultText(realtimeData.getAreaCongestLvl(), DEFAULT_CONGESTION_LEVEL))
                .congestionMessage(defaultText(realtimeData.getAreaCongestMsg(), DEFAULT_CONGESTION_MESSAGE))
                .build();
    }

    private String formatTemperature(Double temperature) {
        if (temperature == null) {
            return DEFAULT_NUMERIC_TEXT;
        }
        return String.format(Locale.US, "%.1f", temperature);
    }

    private String formatPm10(Double pm10) {
        if (pm10 == null) {
            return DEFAULT_NUMERIC_TEXT;
        }
        if (pm10 <= 30) {
            return "\uC88B\uC74C";
        }
        if (pm10 <= 80) {
            return "\uBCF4\uD1B5";
        }
        if (pm10 <= 150) {
            return "\uB098\uC068";
        }
        return "\uB9E4\uC6B0\uB098\uC068";
    }

    private String resolvePm10(SeoulRealtimeData realtimeData) {
        if (StringUtils.hasText(realtimeData.getPm10Status()) && !DEFAULT_NUMERIC_TEXT.equals(realtimeData.getPm10Status())) {
            return realtimeData.getPm10Status();
        }
        return formatPm10(realtimeData.getPm10());
    }

    private String formatPrecipitationProbability(Double rainChance) {
        if (rainChance == null) {
            return DEFAULT_NUMERIC_TEXT;
        }
        int value = (int) Math.round(rainChance);
        return value + "%";
    }

    private String defaultText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private void validateGuName(String guName) {
        if (!StringUtils.hasText(guName) || !VALID_GU_NAMES.contains(guName)) {
            throw new CustomException(ErrorCode.INVALID_GU_NAME);
        }
    }
}
