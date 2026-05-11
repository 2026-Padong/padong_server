package com.example.padong_server.global.client.sk;

import com.example.padong_server.global.exception.CustomException;
import com.example.padong_server.global.exception.ErrorCode;
import com.example.padong_server.global.util.Preconditions;
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
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(SkAddressProperties.class)
public class SkPedestrianRouteClient {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);
    private static final String PEDESTRIAN_PATH = "/tmap/routes/pedestrian?version=1";
    private static final String COORD_TYPE = "WGS84GEO";

    private final SkAddressProperties properties;

    public Map<String, Object> route(
            String startName, double startX, double startY,
            String endName, double endX, double endY) {
        Preconditions.validate(
                StringUtils.hasText(properties.appKey()), ErrorCode.SK_PEDESTRIAN_API_KEY_MISSING);

        String url = properties.baseUrl() + PEDESTRIAN_PATH;
        String body = buildBody(startName, startX, startY, endName, endX, endY);
        HttpResponse<String> response = sendPost(url, body);

        Preconditions.validate(
                response.statusCode() == 200, ErrorCode.SK_PEDESTRIAN_API_CALL_FAILED);

        return parseJson(response.body());
    }

    private String buildBody(
            String startName, double startX, double startY,
            String endName, double endX, double endY) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("startName", urlEncode(startName));
        payload.put("startX", startX);
        payload.put("startY", startY);
        payload.put("endName", urlEncode(endName));
        payload.put("endX", endX);
        payload.put("endY", endY);
        payload.put("reqCoordType", COORD_TYPE);
        payload.put("resCoordType", COORD_TYPE);
        try {
            return OBJECT_MAPPER.writeValueAsString(payload);
        } catch (IOException exception) {
            throw new CustomException(ErrorCode.SK_PEDESTRIAN_API_CALL_FAILED, exception);
        }
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private HttpResponse<String> sendPost(String url, String body) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .header("appKey", properties.appKey())
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            return HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new CustomException(ErrorCode.SK_PEDESTRIAN_API_CALL_FAILED, exception);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJson(String body) {
        try {
            return OBJECT_MAPPER.readValue(body, Map.class);
        } catch (IOException exception) {
            throw new CustomException(ErrorCode.SK_PEDESTRIAN_API_CALL_FAILED, exception);
        }
    }
}
