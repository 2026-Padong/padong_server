package com.example.padong_server.domain.news.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "naver.news")
public record NaverNewsProperties(String clientId, String clientSecret) {}
