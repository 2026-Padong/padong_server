package com.example.padong_server.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai.datasource")
public record AiDataSourceProperties(
        String jdbcUrl,
        String username,
        String password,
        String driverClassName
) {}
