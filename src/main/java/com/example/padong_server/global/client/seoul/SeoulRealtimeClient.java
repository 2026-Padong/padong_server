package com.example.padong_server.global.client.seoul;

import com.example.padong_server.global.exception.CustomException;
import com.example.padong_server.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class SeoulRealtimeClient {

    private static final String DEFAULT_CONGEST_LEVEL = "정보 없음";
    private static final String DEFAULT_CONGEST_MESSAGE = "정보 없음";
    private static final String DEFAULT_WEATHER_STATUS = "정보 없음";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final WebClient seoulRealtimeWebClient;
    private final SeoulRealtimeProperties properties;

    public SeoulRealtimeData getRealtimeDataByAreaNm(String areaNm) {
        validateProperties();

        try {
            String responseBody = seoulRealtimeWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/{apiKey}/json/citydata/1/5/{areaNm}")
                            .build(properties.apiKey(), areaNm))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, response ->
                            response.bodyToMono(String.class)
                                    .defaultIfEmpty("")
                                    .flatMap(body -> Mono.error(new CustomException(
                                            ErrorCode.SEOUL_REALTIME_API_CALL_FAILED,
                                            "서울시 실시간 도시데이터 API 호출에 실패했습니다. status=" + response.statusCode() + ", body=" + body
                                    )))
                    )
                    .bodyToMono(String.class)
                    .timeout(REQUEST_TIMEOUT)
                    .block();

            JsonNode rootNode = responseBody == null ? null : OBJECT_MAPPER.readTree(responseBody);
            if (rootNode == null || rootNode.isNull()) {
                throw new CustomException(ErrorCode.SEOUL_REALTIME_DATA_NOT_FOUND);
            }

            JsonNode targetNode = findTargetAreaNode(rootNode, areaNm);
            if (targetNode == null) {
                throw new CustomException(ErrorCode.SEOUL_REALTIME_DATA_NOT_FOUND);
            }

            JsonNode weatherNode = extractWeatherNode(targetNode);
            JsonNode forecastNode = extractForecastNode(weatherNode);

            return SeoulRealtimeData.builder()
                    .areaCd(readText(targetNode, "AREA_CD", null))
                    .areaNm(readText(targetNode, "AREA_NM", areaNm))
                    .thumbnail(readText(targetNode, "THUMBNAIL", null))
                    .roadAddr(resolveAddress(targetNode))
                    .areaPpltnMin(readDouble(targetNode, "AREA_PPLTN_MIN"))
                    .areaPpltnMax(readDouble(targetNode, "AREA_PPLTN_MAX"))
                    .areaCongestLvl(readText(targetNode, "AREA_CONGEST_LVL", DEFAULT_CONGEST_LEVEL))
                    .areaCongestMsg(readText(targetNode, "AREA_CONGEST_MSG", DEFAULT_CONGEST_MESSAGE))
                    .fcstYn(readText(targetNode, "FCST_YN", null))
                    .fcstPpltnMin(readDouble(targetNode, "FCST_PPLTN_MIN"))
                    .fcstPpltnMax(readDouble(targetNode, "FCST_PPLTN_MAX"))
                    .fcstTime(readText(targetNode, "FCST_TIME", null))
                    .malePpltnRate(readDouble(targetNode, "MALE_PPLTN_RATE"))
                    .femalePpltnRate(readDouble(targetNode, "FEMALE_PPLTN_RATE"))
                    .ppltnRate10(readDouble(targetNode, "PPLTN_RATE_10"))
                    .ppltnRate20(readDouble(targetNode, "PPLTN_RATE_20"))
                    .ppltnRate30(readDouble(targetNode, "PPLTN_RATE_30"))
                    .ppltnRate40(readDouble(targetNode, "PPLTN_RATE_40"))
                    .ppltnRate50(readDouble(targetNode, "PPLTN_RATE_50"))
                    .ppltnRate60(readDouble(targetNode, "PPLTN_RATE_60"))
                    .ppltnRate70(readDouble(targetNode, "PPLTN_RATE_70"))
                    .roadTrafficIdx(readText(targetNode, "ROAD_TRAFFIC_IDX", null))
                    .roadTrafficSpd(readDouble(targetNode, "ROAD_TRAFFIC_SPD"))
                    .weatherStatus(extractWeatherStatus(weatherNode, forecastNode))
                    .temperature(readDouble(weatherNode, "TEMP"))
                    .sensibleTemperature(readDouble(weatherNode, "SENSIBLE_TEMP"))
                    .humidity(readDouble(weatherNode, "HUMIDITY"))
                    .pm10(readDouble(weatherNode, "PM10"))
                    .pm10Status(readText(weatherNode, "PM10_INDEX", DEFAULT_CONGEST_LEVEL))
                    .rainChance(readDouble(forecastNode, "RAIN_CHANCE"))
                    .eventNm(readText(targetNode, "EVENT_NM", null))
                    .subwayStationNames(extractDistinctTexts(targetNode, "SUB_STTS", "SUB_STN_NM"))
                    .subwayLines(extractDistinctTexts(targetNode, "SUB_STTS", "SUB_LINE"))
                    .busStopNames(extractDistinctTexts(targetNode, "BUS_STN_STTS", "BUS_STN_NM"))
                    .bikeStationNames(extractDistinctTexts(targetNode, "SBIKE_STTS", "SBIKE_SPOT_NM"))
                    .build();
        } catch (CustomException exception) {
            throw exception;
        } catch (WebClientResponseException exception) {
            throw new CustomException(
                    ErrorCode.SEOUL_REALTIME_API_CALL_FAILED,
                    "서울시 실시간 도시데이터 API 호출에 실패했습니다. areaNm=" + areaNm,
                    exception
            );
        } catch (Exception exception) {
            throw new CustomException(
                    ErrorCode.SEOUL_REALTIME_API_CALL_FAILED,
                    "서울시 실시간 도시데이터 API 처리 중 오류가 발생했습니다. areaNm=" + areaNm,
                    exception
            );
        }
    }

    private void validateProperties() {
        if (!StringUtils.hasText(properties.baseUrl()) || !StringUtils.hasText(properties.apiKey())) {
            throw new CustomException(
                    ErrorCode.SEOUL_REALTIME_API_CALL_FAILED,
                    "seoul-open-api.base-url 또는 seoul-open-api.api-key 설정이 비어 있습니다."
            );
        }
    }

    private JsonNode findTargetAreaNode(JsonNode node, String areaNm) {
        if (node == null || node.isNull()) {
            return null;
        }

        if (node.isObject()) {
            String currentAreaNm = asText(node.get("AREA_NM"));
            if (areaNm.equals(currentAreaNm)) {
                return node;
            }

            Iterator<JsonNode> children = node.elements();
            while (children.hasNext()) {
                JsonNode found = findTargetAreaNode(children.next(), areaNm);
                if (found != null) {
                    return found;
                }
            }
        }

        if (node.isArray()) {
            for (JsonNode child : node) {
                JsonNode found = findTargetAreaNode(child, areaNm);
                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }

    private String readText(JsonNode targetNode, String fieldName, String defaultValue) {
        JsonNode fieldNode = findField(targetNode, fieldName);
        String value = asText(fieldNode);
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private String resolveAddress(JsonNode targetNode) {
        String areaRoadAddress = readText(targetNode, "ROAD_ADDR", null);
        if (StringUtils.hasText(areaRoadAddress)) {
            return areaRoadAddress;
        }

        JsonNode parkingStatusNode = targetNode.get("PRK_STTS");
        if (parkingStatusNode != null && parkingStatusNode.isArray() && !parkingStatusNode.isEmpty()) {
            JsonNode firstParkingNode = parkingStatusNode.get(0);
            String parkingRoadAddress = readText(firstParkingNode, "ROAD_ADDR", null);
            if (StringUtils.hasText(parkingRoadAddress)) {
                return parkingRoadAddress;
            }

            return readText(firstParkingNode, "ADDRESS", null);
        }

        return null;
    }

    private JsonNode extractWeatherNode(JsonNode targetNode) {
        JsonNode weatherField = findField(targetNode, "WEATHER_STTS");
        if (weatherField == null || weatherField.isNull()) {
            return null;
        }
        if (weatherField.isArray() && !weatherField.isEmpty()) {
            return weatherField.get(0);
        }
        return weatherField;
    }

    private JsonNode extractForecastNode(JsonNode weatherNode) {
        JsonNode forecastField = findField(weatherNode, "FCST24HOURS");
        if (forecastField == null || forecastField.isNull()) {
            return null;
        }
        if (forecastField.isArray() && !forecastField.isEmpty()) {
            return forecastField.get(0);
        }
        return forecastField;
    }

    private String extractWeatherStatus(JsonNode weatherNode, JsonNode forecastNode) {
        String precipitationType = readText(weatherNode, "PRECPT_TYPE", "");
        if (StringUtils.hasText(precipitationType) && !"없음".equals(precipitationType)) {
            return precipitationType;
        }

        String skyStatus = readText(forecastNode, "SKY_STTS", "");
        if (StringUtils.hasText(skyStatus)) {
            return skyStatus;
        }

        return DEFAULT_WEATHER_STATUS;
    }

    private List<String> extractDistinctTexts(JsonNode targetNode, String collectionFieldName, String itemFieldName) {
        JsonNode collectionNode = findField(targetNode, collectionFieldName);
        if (collectionNode == null || collectionNode.isNull()) {
            return List.of();
        }

        Set<String> values = new LinkedHashSet<>();
        if (collectionNode.isArray()) {
            for (JsonNode item : collectionNode) {
                String value = readText(item, itemFieldName, null);
                if (StringUtils.hasText(value)) {
                    values.add(value.trim());
                }
            }
        } else if (collectionNode.isObject()) {
            String value = readText(collectionNode, itemFieldName, null);
            if (StringUtils.hasText(value)) {
                values.add(value.trim());
            }
        }

        return new ArrayList<>(values);
    }

    private Double readDouble(JsonNode targetNode, String fieldName) {
        JsonNode fieldNode = findField(targetNode, fieldName);
        if (fieldNode == null || fieldNode.isNull()) {
            return null;
        }

        if (fieldNode.isNumber()) {
            return fieldNode.doubleValue();
        }

        String value = fieldNode.asText();
        if (!StringUtils.hasText(value)) {
            return null;
        }

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private JsonNode findField(JsonNode node, String fieldName) {
        if (node == null || node.isNull()) {
            return null;
        }

        if (node.isObject()) {
            JsonNode current = node.get(fieldName);
            if (current != null && !current.isNull()) {
                return current;
            }

            Iterator<JsonNode> children = node.elements();
            while (children.hasNext()) {
                JsonNode found = findField(children.next(), fieldName);
                if (found != null) {
                    return found;
                }
            }
        }

        if (node.isArray()) {
            for (JsonNode child : node) {
                JsonNode found = findField(child, fieldName);
                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }

    private String asText(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        return node.asText();
    }
}
