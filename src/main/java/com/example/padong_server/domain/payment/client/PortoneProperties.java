package com.example.padong_server.domain.payment.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "portone")
public record PortoneProperties(String baseUrl, String apiSecret) {}
