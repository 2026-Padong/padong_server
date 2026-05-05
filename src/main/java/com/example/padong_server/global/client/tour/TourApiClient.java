package com.example.padong_server.global.client.tour;

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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(TourApiProperties.class)
public class TourApiClient {

    private static final String MOBILE_OS = "ETC";
    private static final String MOBILE_APP = "apiTest";
    private static final String RESPONSE_TYPE = "json";
    private static final String SEOUL_AREA_CODE = "1";
    private static final int PAGE_SIZE = 100;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    private final TourApiProperties properties;

    public TourContentPageResult getSeoulContents(int maxRequestCount) {
        ensureServiceKey();

        List<TourContent> contents = new ArrayList<>();
        int totalCount = Integer.MAX_VALUE;
        int pageNo = 1;
        int requestCount = 0;

        while ((pageNo - 1) * PAGE_SIZE < totalCount && requestCount < maxRequestCount) {
            Map<String, Object> responseBody = requestAreaBasedList(pageNo);
            requestCount++;

            Map<String, Object> body = extractBody(responseBody);
            totalCount = parseInt(body.get("totalCount"), totalCount);
            List<Map<String, Object>> items = extractItems(body);

            for (Map<String, Object> item : items) {
                String address = stringValue(item.get("addr1"));
                if (!address.startsWith("서울특별시")) {
                    continue;
                }

                contents.add(new TourContent(
                        stringValue(item.get("contentid")),
                        stringValue(item.get("title")),
                        address,
                        stringValue(item.get("firstimage")),
                        stringValue(item.get("firstimage2"))
                ));
            }

            if (items.isEmpty()) {
                break;
            }

            pageNo++;
        }

        return new TourContentPageResult(List.copyOf(contents), requestCount);
    }

    private Map<String, Object> requestAreaBasedList(int pageNo) {
        try {
            String requestUrl = properties.baseUrl()
                    + "/B551011/KorService2/areaBasedList2"
                    + "?serviceKey=" + encode(properties.serviceKey())
                    + "&numOfRows=" + PAGE_SIZE
                    + "&pageNo=" + pageNo
                    + "&MobileOS=" + encode(MOBILE_OS)
                    + "&MobileApp=" + encode(MOBILE_APP)
                    + "&_type=" + encode(RESPONSE_TYPE)
                    + "&areaCode=" + encode(SEOUL_AREA_CODE);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(requestUrl))
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new CustomException(
                        ErrorCode.TOUR_API_CALL_FAILED,
                        "Tour API HTTP 오류: " + response.statusCode()
                );
            }

            return OBJECT_MAPPER.readValue(response.body(), Map.class);
        } catch (IOException | InterruptedException exception) {
            throw new CustomException(ErrorCode.TOUR_API_CALL_FAILED, exception);
        } catch (CustomException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new CustomException(ErrorCode.TOUR_API_CALL_FAILED, exception);
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private void ensureServiceKey() {
        if (!StringUtils.hasText(properties.serviceKey())) {
            throw new CustomException(ErrorCode.TOUR_API_KEY_MISSING);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractBody(Map<String, Object> responseBody) {
        Map<String, Object> response = castMap(responseBody.get("response"));
        Map<String, Object> header = castMap(response.get("header"));

        String resultCode = stringValue(header.get("resultCode"));
        if (StringUtils.hasText(resultCode) && !resultCode.equals("0000") && !resultCode.equals("00")) {
            throw new CustomException(
                    ErrorCode.TOUR_API_CALL_FAILED,
                    "Tour API 오류: " + stringValue(header.get("resultMsg"))
            );
        }

        return castMap(response.get("body"));
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractItems(Map<String, Object> body) {
        if (body == null) {
            return List.of();
        }

        Map<String, Object> items = castMap(body.get("items"));
        if (items.isEmpty()) {
            return List.of();
        }

        Object item = items.get("item");
        if (item == null) {
            return List.of();
        }
        if (item instanceof Map<?, ?> single) {
            return List.of((Map<String, Object>) single);
        }
        if (item instanceof Collection<?> collection) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (Object element : collection) {
                if (element instanceof Map<?, ?> map) {
                    list.add((Map<String, Object>) map);
                }
            }
            return list;
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    private int parseInt(Object value, int defaultValue) {
        String text = stringValue(value);
        if (!StringUtils.hasText(text)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    private String stringValue(Object value) {
        return Objects.toString(value, "");
    }

    public record TourContentPageResult(
            List<TourContent> contents,
            int requestCount
    ) {
    }
}
