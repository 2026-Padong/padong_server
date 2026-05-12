package com.example.padong_server.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Configuration
public class OpenApiConfig {

    @Value("${swagger-config.server-url-http:http://localhost:8080}")
    private String swaggerServerUrlHttp;

    @Value("${swagger-config.server-url-https:http://localhost:8080}")
    private String swaggerServerUrlHttps;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .servers(swaggerServerUrls().stream()
                        .map(url -> new Server().url(url))
                        .toList());
    }

    private List<String> swaggerServerUrls() {
        Set<String> serverUrls = new LinkedHashSet<>();
        addServerUrl(serverUrls, swaggerServerUrlHttps);
        addServerUrl(serverUrls, swaggerServerUrlHttp);
        return List.copyOf(serverUrls);
    }

    private void addServerUrl(Set<String> serverUrls, String serverUrl) {
        if (serverUrl != null && !serverUrl.isBlank()) {
            serverUrls.add(serverUrl.trim());
        }
    }
}
