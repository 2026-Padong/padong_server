package com.example.padong_server.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Swagger UI 서버 URL 설정 (HTTP/HTTPS). */
@ConfigurationProperties(prefix = "swagger-config")
public record SwaggerProperties(String serverUrlHttp, String serverUrlHttps) {

    public SwaggerProperties {
        if (serverUrlHttp == null || serverUrlHttp.isBlank()) {
            serverUrlHttp = "http://localhost:8080";
        }
        if (serverUrlHttps == null || serverUrlHttps.isBlank()) {
            serverUrlHttps = "http://localhost:8080";
        }
    }
}
