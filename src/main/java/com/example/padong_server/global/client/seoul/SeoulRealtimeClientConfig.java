package com.example.padong_server.global.client.seoul;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(SeoulRealtimeProperties.class)
public class SeoulRealtimeClientConfig {

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    public WebClient seoulRealtimeWebClient(WebClient.Builder webClientBuilder,
                                            SeoulRealtimeProperties properties) {
        return webClientBuilder
                .baseUrl(properties.baseUrl())
                .build();
    }
}
