package com.example.padong_server.domain.oauth.service;

import java.time.Duration;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final String KEY_PREFIX = "refresh-token:";

    private final StringRedisTemplate redisTemplate;

    public void save(Long userId, String refreshToken, long expirationMillis) {
        redisTemplate.opsForValue().set(
                getKey(userId),
                refreshToken,
                Duration.ofMillis(expirationMillis)
        );
    }

    public boolean matches(Long userId, String refreshToken) {
        String savedToken = redisTemplate.opsForValue().get(getKey(userId));
        return refreshToken.equals(savedToken);
    }

    public void delete(Long userId) {
        redisTemplate.delete(getKey(userId));
    }

    private String getKey(Long userId) {
        return KEY_PREFIX + userId;
    }
}
