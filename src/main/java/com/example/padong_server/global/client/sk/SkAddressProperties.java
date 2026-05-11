package com.example.padong_server.global.client.sk;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sk-address-api")
public record SkAddressProperties(
        String baseUrl,
        String appKey
) {
}
