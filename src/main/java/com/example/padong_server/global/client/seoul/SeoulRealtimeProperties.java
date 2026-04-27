package com.example.padong_server.global.client.seoul;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "seoul-open-api")
public record SeoulRealtimeProperties(
        String baseUrl,
        String apiKey
) {
}
