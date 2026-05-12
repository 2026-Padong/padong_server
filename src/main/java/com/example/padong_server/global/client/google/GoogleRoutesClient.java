package com.example.padong_server.global.client.google;

import com.example.padong_server.domain.path.dto.internal.PathSummary;
import com.example.padong_server.domain.path.entity.PathSource;
import com.example.padong_server.global.exception.CustomException;
import com.example.padong_server.global.exception.ErrorCode;
import com.example.padong_server.global.util.Preconditions;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import tools.jackson.core.JacksonException;

@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(GoogleRoutesProperties.class)
public class GoogleRoutesClient {

    public enum TravelMode {
        TRANSIT, WALK, DRIVE
    }

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);
    private static final String COMPUTE_ROUTES_PATH = "/directions/v2:computeRoutes";
    private static final String FIELD_MASK = "routes.duration,routes.distanceMeters";
    private static final int SECONDS_PER_MINUTE = 60;

    private final GoogleRoutesProperties properties;

    public PathSummary route(
            TravelMode mode,
            double originLatitude,
            double originLongitude,
            double destinationLatitude,
            double destinationLongitude) {
        Preconditions.validate(
                StringUtils.hasText(properties.apiKey()),
                ErrorCode.GOOGLE_ROUTES_API_KEY_MISSING);

        String url = properties.baseUrl() + COMPUTE_ROUTES_PATH;
        String body = buildBody(
                mode, originLatitude, originLongitude, destinationLatitude, destinationLongitude);
        HttpResponse<String> response = sendPost(url, body);

        Preconditions.validate(
                response.statusCode() == 200, ErrorCode.GOOGLE_ROUTES_API_CALL_FAILED);

        return parseSummary(response.body());
    }

    private String buildBody(
            TravelMode mode,
            double originLatitude,
            double originLongitude,
            double destinationLatitude,
            double destinationLongitude) {
        Map<String, Object> origin = location(originLatitude, originLongitude);
        Map<String, Object> destination = location(destinationLatitude, destinationLongitude);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("origin", origin);
        payload.put("destination", destination);
        payload.put("travelMode", mode.name());
        payload.put("languageCode", "ko");
        payload.put("units", "METRIC");
        try {
            return OBJECT_MAPPER.writeValueAsString(payload);
        } catch (JacksonException exception) {
            throw new CustomException(ErrorCode.GOOGLE_ROUTES_API_CALL_FAILED, exception);
        }
    }

    private Map<String, Object> location(double latitude, double longitude) {
        Map<String, Object> latLng = new LinkedHashMap<>();
        latLng.put("latitude", latitude);
        latLng.put("longitude", longitude);
        Map<String, Object> location = new LinkedHashMap<>();
        location.put("latLng", latLng);
        Map<String, Object> wrapper = new LinkedHashMap<>();
        wrapper.put("location", location);
        return wrapper;
    }

    private HttpResponse<String> sendPost(String url, String body) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Content-Type", "application/json")
                    .header("X-Goog-Api-Key", properties.apiKey())
                    .header("X-Goog-FieldMask", FIELD_MASK)
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            return HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new CustomException(ErrorCode.GOOGLE_ROUTES_API_CALL_FAILED, exception);
        }
    }

    @SuppressWarnings("unchecked")
    private PathSummary parseSummary(String body) {
        Map<String, Object> parsed;
        try {
            parsed = OBJECT_MAPPER.readValue(body, Map.class);
        } catch (JacksonException exception) {
            throw new CustomException(ErrorCode.GOOGLE_ROUTES_API_CALL_FAILED, exception);
        }

        Object routesNode = parsed.get("routes");
        if (!(routesNode instanceof List<?> routes) || routes.isEmpty()) {
            throw new CustomException(ErrorCode.GOOGLE_ROUTES_NO_RESULT);
        }
        Object first = routes.get(0);
        if (!(first instanceof Map<?, ?> route)) {
            throw new CustomException(ErrorCode.GOOGLE_ROUTES_NO_RESULT);
        }

        int totalTime = parseDurationToMinutes(
                Objects.toString(((Map<String, Object>) route).get("duration"), ""));
        int totalDistance = intValue(((Map<String, Object>) route).get("distanceMeters"));
        return new PathSummary(totalTime, totalDistance, PathSource.GOOGLE);
    }

    private int parseDurationToMinutes(String duration) {
        // Google Routes returns duration as protobuf duration string e.g. "1234s"
        if (duration == null || duration.isBlank()) {
            return 0;
        }
        String trimmed = duration.endsWith("s") ? duration.substring(0, duration.length() - 1) : duration;
        try {
            int seconds = (int) Math.round(Double.parseDouble(trimmed));
            return (int) Math.round((double) seconds / SECONDS_PER_MINUTE);
        } catch (NumberFormatException exception) {
            throw new CustomException(ErrorCode.GOOGLE_ROUTES_API_CALL_FAILED, exception);
        }
    }

    private int intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return 0;
    }
}
