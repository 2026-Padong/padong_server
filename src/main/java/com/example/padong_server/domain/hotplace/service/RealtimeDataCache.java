package com.example.padong_server.domain.hotplace.service;

import com.example.padong_server.global.client.seoul.SeoulRealtimeClient;
import com.example.padong_server.global.client.seoul.SeoulRealtimeData;
import com.example.padong_server.global.exception.CustomException;
import com.example.padong_server.global.exception.ErrorCode;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Hotplace 실시간 데이터 read-through 캐시.
 *
 * <p>키별 5분 TTL. 만료 또는 miss 시 외부 API 호출 후 갱신.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RealtimeDataCache {

    private static final String KEY_PREFIX = "realtime:hotplace:";
    private static final Duration TTL = Duration.ofMinutes(5);

    private final StringRedisTemplate redisTemplate;
    private final SeoulRealtimeClient seoulRealtimeClient;
    private final ObjectMapper objectMapper;

    public SeoulRealtimeData getOrFetch(String areaNm) {
        String key = key(areaNm);
        String cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, SeoulRealtimeData.class);
            } catch (JacksonException e) {
                log.warn("Failed to deserialize cached realtime data for areaNm={}", areaNm, e);
                redisTemplate.delete(key);
            }
        }
        SeoulRealtimeData fresh = seoulRealtimeClient.getRealtimeDataByAreaNm(areaNm);
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(fresh), TTL);
        } catch (JacksonException e) {
            log.warn("Failed to cache realtime data for areaNm={}", areaNm, e);
        }
        return fresh;
    }

    private String key(String areaNm) {
        return KEY_PREFIX + areaNm;
    }
}
