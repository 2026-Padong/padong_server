package com.example.padong_server.global.client.seoul;

import com.example.padong_server.global.exception.CustomException;
import com.example.padong_server.global.exception.ErrorCode;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
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
                            .pathSegment(
                                    properties.apiKey(),
                                    "json",
                                    "citydata",
                                    "1",
                                    "5",
                                    areaNm)
                            .build())
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
                throw new CustomException(
                        ErrorCode.SEOUL_REALTIME_DATA_NOT_FOUND,
                        "서울시 실시간 API 빈 응답. areaNm=" + areaNm);
            }

            JsonNode targetNode = findTargetAreaNode(rootNode, areaNm);
            if (targetNode == null) {
                // 서울 API 는 200 OK 라도 RESULT.CODE 로 실패 알려줌 — 사람이 볼 수 있게 본문 포함
                String resultCode = readResultText(rootNode, "RESULT.CODE");
                String resultMsg = readResultText(rootNode, "RESULT.MESSAGE");
                throw new CustomException(
                        ErrorCode.SEOUL_REALTIME_DATA_NOT_FOUND,
                        "서울시 실시간 API 응답에 AREA_NM 매칭 없음. areaNm="
                                + areaNm
                                + ", RESULT.CODE="
                                + resultCode
                                + ", RESULT.MESSAGE="
                                + resultMsg);
            }

            JsonNode weatherNode = extractWeatherNode(targetNode);
            JsonNode forecastNode = extractForecastNode(weatherNode);

            return new SeoulRealtimeData(
                    readText(targetNode, "AREA_CD", null),
                    readText(targetNode, "AREA_NM", areaNm),
                    readText(targetNode, "THUMBNAIL", null),
                    resolveAddress(targetNode),
                    readDouble(targetNode, "AREA_PPLTN_MIN"),
                    readDouble(targetNode, "AREA_PPLTN_MAX"),
                    readText(targetNode, "AREA_CONGEST_LVL", DEFAULT_CONGEST_LEVEL),
                    readText(targetNode, "AREA_CONGEST_MSG", DEFAULT_CONGEST_MESSAGE),
                    readText(targetNode, "FCST_YN", null),
                    readDouble(targetNode, "FCST_PPLTN_MIN"),
                    readDouble(targetNode, "FCST_PPLTN_MAX"),
                    readText(targetNode, "FCST_TIME", null),
                    readDouble(targetNode, "MALE_PPLTN_RATE"),
                    readDouble(targetNode, "FEMALE_PPLTN_RATE"),
                    readDouble(targetNode, "PPLTN_RATE_10"),
                    readDouble(targetNode, "PPLTN_RATE_20"),
                    readDouble(targetNode, "PPLTN_RATE_30"),
                    readDouble(targetNode, "PPLTN_RATE_40"),
                    readDouble(targetNode, "PPLTN_RATE_50"),
                    readDouble(targetNode, "PPLTN_RATE_60"),
                    readDouble(targetNode, "PPLTN_RATE_70"),
                    readText(targetNode, "ROAD_TRAFFIC_IDX", null),
                    readDouble(targetNode, "ROAD_TRAFFIC_SPD"),
                    extractWeatherStatus(weatherNode, forecastNode),
                    readDouble(weatherNode, "TEMP"),
                    readDouble(weatherNode, "SENSIBLE_TEMP"),
                    readDouble(weatherNode, "HUMIDITY"),
                    readDouble(weatherNode, "PM10"),
                    readText(weatherNode, "PM10_INDEX", DEFAULT_CONGEST_LEVEL),
                    readDouble(forecastNode, "RAIN_CHANCE"),
                    readText(targetNode, "EVENT_NM", null),
                    extractDistinctTexts(targetNode, "SUB_STTS", "SUB_STN_NM"),
                    extractDistinctTexts(targetNode, "SUB_STTS", "SUB_LINE"),
                    extractDistinctTexts(targetNode, "BUS_STN_STTS", "BUS_STN_NM"),
                    extractDistinctTexts(targetNode, "SBIKE_STTS", "SBIKE_SPOT_NM"),
                    readText(targetNode, "PPLTN_TIME", null));
        } catch (CustomException exception) {
            throw exception;
        } catch (WebClientResponseException exception) {
            log.warn(
                    "[SeoulRealtime] HTTP error. areaNm={}, status={}, body={}",
                    areaNm,
                    exception.getStatusCode(),
                    exception.getResponseBodyAsString());
            throw new CustomException(
                    ErrorCode.SEOUL_REALTIME_API_CALL_FAILED,
                    "서울시 실시간 도시데이터 API 호출에 실패했습니다. areaNm=" + areaNm,
                    exception
            );
        } catch (Exception exception) {
            log.warn(
                    "[SeoulRealtime] 처리 실패. areaNm={}, ex={}, msg={}",
                    areaNm,
                    exception.getClass().getSimpleName(),
                    exception.getMessage(),
                    exception);
            throw new CustomException(
                    ErrorCode.SEOUL_REALTIME_API_CALL_FAILED,
                    "서울시 실시간 도시데이터 API 처리 중 오류가 발생했습니다. areaNm=" + areaNm
                            + ", cause=" + exception.getClass().getSimpleName() + ": "
                            + exception.getMessage(),
                    exception
            );
        }
    }

    private void validateProperties() {
        String baseUrl = properties.baseUrl();
        String apiKey = properties.apiKey();
        if (!StringUtils.hasText(baseUrl) || !StringUtils.hasText(apiKey)) {
            throw new CustomException(
                    ErrorCode.SEOUL_REALTIME_API_CALL_FAILED,
                    "seoul-open-api.base-url 또는 seoul-open-api.api-key 설정이 비어 있습니다."
            );
        }
        // env 가 resolve 안 돼서 `${VAR}` 리터럴이 박혀 있으면 WebClient 가 URI placeholder 로 오인 → 미리 차단
        if (baseUrl.contains("${") || apiKey.contains("${")) {
            throw new CustomException(
                    ErrorCode.SEOUL_REALTIME_API_CALL_FAILED,
                    "seoul-open-api 설정에 미해결 환경변수 placeholder 가 남아 있습니다. "
                            + "SEOUL_OPEN_API_BASE_URL / SEOUL_OPEN_API_KEY 환경변수를 확인하세요. "
                            + "baseUrl=" + baseUrl);
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

            Iterator<JsonNode> children = node.iterator();
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

    /** 서울 API 의 RESULT.CODE / RESULT.MESSAGE 같이 점 포함 필드명을 안전하게 읽어옴. */
    private String readResultText(JsonNode root, String dottedKey) {
        if (root == null) {
            return null;
        }
        JsonNode resultNode = root.get("RESULT");
        if (resultNode == null || resultNode.isNull()) {
            // RESULT 가 root 가 아니라 그 안 어딘가에 있을 수 있음 — 재귀 탐색
            JsonNode found = findField(root, dottedKey);
            return asText(found);
        }
        JsonNode value = resultNode.get(dottedKey);
        return asText(value);
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

            Iterator<JsonNode> children = node.iterator();
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
