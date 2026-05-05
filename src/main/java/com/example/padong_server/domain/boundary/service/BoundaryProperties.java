package com.example.padong_server.domain.boundary.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "boundary-api")
public record BoundaryProperties(
        String baseUrl,
        String serviceKey,
        String domain
) {
}
