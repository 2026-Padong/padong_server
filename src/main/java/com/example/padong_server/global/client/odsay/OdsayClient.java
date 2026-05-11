package com.example.padong_server.global.client.odsay;

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
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(OdsayProperties.class)
public class OdsayClient {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);
    private static final String SEARCH_PUB_TRANS_PATH = "/v1/api/searchPubTransPathT";

    private final OdsayProperties properties;

    public Map<String, Object> searchPubTransPath(
            double sx, double sy, double ex, double ey, Integer opt, Integer searchPathType) {
        Preconditions.validate(
                StringUtils.hasText(properties.apiKey()), ErrorCode.ODSAY_API_KEY_MISSING);

        String url = buildUrl(sx, sy, ex, ey, opt, searchPathType);
        HttpResponse<String> response = sendGet(url);

        Preconditions.validate(response.statusCode() == 200, ErrorCode.ODSAY_API_CALL_FAILED);

        Map<String, Object> body = parseJson(response.body());
        validateOdsayError(body);
        return body;
    }

    private String buildUrl(
            double sx, double sy, double ex, double ey, Integer opt, Integer searchPathType) {
        StringBuilder url = new StringBuilder(properties.baseUrl())
                .append(SEARCH_PUB_TRANS_PATH)
                .append("?SX=").append(formatCoord(sx))
                .append("&SY=").append(formatCoord(sy))
                .append("&EX=").append(formatCoord(ex))
                .append("&EY=").append(formatCoord(ey))
                .append("&SearchType=0")
                .append("&apiKey=")
                .append(URLEncoder.encode(properties.apiKey(), StandardCharsets.UTF_8));
        if (opt != null) {
            url.append("&OPT=").append(opt);
        }
        if (searchPathType != null) {
            url.append("&SearchPathType=").append(searchPathType);
        }
        return url.toString();
    }

    private String formatCoord(double value) {
        return String.format(Locale.ROOT, "%.7f", value);
    }

    private HttpResponse<String> sendGet(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Content-type", "application/json")
                    .GET()
                    .build();
            return HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new CustomException(ErrorCode.ODSAY_API_CALL_FAILED, exception);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJson(String body) {
        try {
            return OBJECT_MAPPER.readValue(body, Map.class);
        } catch (IOException exception) {
            throw new CustomException(ErrorCode.ODSAY_API_CALL_FAILED, exception);
        }
    }

    @SuppressWarnings("unchecked")
    private void validateOdsayError(Map<String, Object> body) {
        if (!(body.get("error") instanceof Map<?, ?> errorMap)) {
            return;
        }
        Map<String, Object> error = (Map<String, Object>) errorMap;
        String code = Objects.toString(error.get("code"), "").trim();
        String message = Objects.toString(error.get("msg"), "");
        throw new CustomException(
                mapOdsayErrorCode(code), "ODsay error " + code + ": " + message);
    }

    private ErrorCode mapOdsayErrorCode(String code) {
        return switch (code) {
            case "500" -> ErrorCode.ODSAY_API_CALL_FAILED;
            case "-8", "-9" -> ErrorCode.ODSAY_API_INVALID_PARAM;
            case "3" -> ErrorCode.ODSAY_NO_DEPARTURE_STATION;
            case "4" -> ErrorCode.ODSAY_NO_ARRIVAL_STATION;
            case "5" -> ErrorCode.ODSAY_NO_STATION;
            case "6" -> ErrorCode.ODSAY_OUT_OF_SERVICE_AREA;
            case "-98" -> ErrorCode.ODSAY_TOO_CLOSE;
            case "-99" -> ErrorCode.ODSAY_NO_RESULT;
            default -> ErrorCode.ODSAY_API_CALL_FAILED;
        };
    }
}
