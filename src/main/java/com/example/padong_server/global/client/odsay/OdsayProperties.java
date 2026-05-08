package com.example.padong_server.global.client.odsay;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "odsay-api")
public record OdsayProperties(
        String baseUrl,
        String apiKey
) {
}
