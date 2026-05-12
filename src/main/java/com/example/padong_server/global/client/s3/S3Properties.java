package com.example.padong_server.global.client.s3;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "s3")
public record S3Properties(
        @NotBlank String bucket,
        String prefix
) {
}
