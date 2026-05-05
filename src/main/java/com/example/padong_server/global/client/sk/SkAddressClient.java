package com.example.padong_server.global.client.sk;

import com.example.padong_server.global.exception.CustomException;
import com.example.padong_server.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(SkAddressProperties.class)
public class SkAddressClient {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    private final SkAddressProperties properties;

    public SkAddress resolveRoadAddress(String roadAddress) {
        if (!StringUtils.hasText(properties.appKey())) {
            throw new CustomException(ErrorCode.ADDRESS_API_KEY_MISSING);
        }

        try {
            String requestUrl = properties.baseUrl()
                    + "/tmap/pois?version=1&format=json&count=1&searchKeyword="
                    + URLEncoder.encode(roadAddress, StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(requestUrl))
                    .header("appKey", properties.appKey())
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new CustomException(
                        ErrorCode.ADDRESS_API_CALL_FAILED,
                        "주소 API HTTP 오류: " + response.statusCode()
                );
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> responseBody = OBJECT_MAPPER.readValue(response.body(), Map.class);
            return extractAddress(roadAddress, responseBody);
        } catch (IOException | InterruptedException exception) {
            throw new CustomException(ErrorCode.ADDRESS_API_CALL_FAILED, exception);
        } catch (CustomException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new CustomException(ErrorCode.ADDRESS_API_CALL_FAILED, exception);
        }
    }

    @SuppressWarnings("unchecked")
    private SkAddress extractAddress(String roadAddress, Map<String, Object> responseBody) {
        if (responseBody == null || responseBody.isEmpty()) {
            return null;
        }

        Map<String, Object> searchPoiInfo = castMap(responseBody.get("searchPoiInfo"));
        Map<String, Object> pois = castMap(searchPoiInfo.get("pois"));
        Object poiNode = pois.get("poi");
        if (poiNode == null) {
            return null;
        }

        Map<String, Object> poi = null;
        if (poiNode instanceof Map<?, ?> single) {
            poi = (Map<String, Object>) single;
        } else if (poiNode instanceof Collection<?> collection) {
            for (Object element : collection) {
                if (element instanceof Map<?, ?> map) {
                    poi = (Map<String, Object>) map;
                    break;
                }
            }
        }

        if (poi == null || poi.isEmpty()) {
            return null;
        }

        String jibunAddress = joinNonBlank(
                stringValue(poi.get("upperAddrName")),
                stringValue(poi.get("middleAddrName")),
                stringValue(poi.get("lowerAddrName")),
                stringValue(poi.get("detailAddrName")),
                joinBuildingNumbers(
                        stringValue(poi.get("firstNo")),
                        stringValue(poi.get("secondNo"))
                )
        );

        return new SkAddress(
                roadAddress,
                jibunAddress,
                stringValue(poi.get("lowerAddrName")),
                stringValue(poi.get("adminDongCode"))
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    private String joinBuildingNumbers(String firstNo, String secondNo) {
        if (!StringUtils.hasText(firstNo)) {
            return "";
        }
        if (!StringUtils.hasText(secondNo) || "0".equals(secondNo)) {
            return firstNo;
        }
        return firstNo + "-" + secondNo;
    }

    private String joinNonBlank(String... parts) {
        return List.of(parts).stream()
                .filter(StringUtils::hasText)
                .map(this::normalize)
                .reduce((left, right) -> left + " " + right)
                .orElse("");
    }

    private String stringValue(Object value) {
        return normalize(Objects.toString(value, ""));
    }

    private String normalize(String value) {
        return Objects.toString(value, "").replaceAll("\\s+", " ").trim();
    }
}
