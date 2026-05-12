package com.example.padong_server.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class OpenApiConfig {

    private final SwaggerProperties swaggerProperties;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("Padong API")
                                .description("파동 백엔드 API 명세")
                                .version("v1"))
                .servers(swaggerServerUrls().stream()
                        .map(url -> new Server().url(url))
                        .toList())
                .components(
                        new Components()
                                .addSecuritySchemes(
                                        "bearer-jwt",
                                        new SecurityScheme()
                                                .type(SecurityScheme.Type.HTTP)
                                                .scheme("bearer")
                                                .bearerFormat("JWT")
                                                .in(SecurityScheme.In.HEADER)
                                                .name("Authorization")));
    }

    private List<String> swaggerServerUrls() {
        Set<String> serverUrls = new LinkedHashSet<>();
        addServerUrl(serverUrls, swaggerProperties.serverUrlHttps());
        addServerUrl(serverUrls, swaggerProperties.serverUrlHttp());
        return List.copyOf(serverUrls);
    }

    private void addServerUrl(Set<String> serverUrls, String serverUrl) {
        if (serverUrl != null && !serverUrl.isBlank()) {
            serverUrls.add(serverUrl.trim());
        }
    }
}
