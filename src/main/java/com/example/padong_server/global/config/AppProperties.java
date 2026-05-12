package com.example.padong_server.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Oauth2 oauth2, Cors cors) {

    public record Oauth2(String frontRedirect) {}

    public record Cors(String allowedOrigins) {}
}
