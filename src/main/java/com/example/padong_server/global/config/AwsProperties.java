package com.example.padong_server.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aws")
public record AwsProperties(String region, String accessKey, String secretKey, S3 s3) {

    public record S3(String bucket, Upload upload) {}

    public record Upload(long maxSizeMb, String allowedContentTypes) {}
}
