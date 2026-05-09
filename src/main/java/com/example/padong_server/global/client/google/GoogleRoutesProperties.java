package com.example.padong_server.global.client.google;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "google-routes-api")
public record GoogleRoutesProperties(
        String baseUrl,
        String apiKey
) {
}
