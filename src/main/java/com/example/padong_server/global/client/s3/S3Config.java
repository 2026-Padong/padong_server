package com.example.padong_server.global.client.s3;

import com.example.padong_server.global.config.AwsProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class S3Config {

    /**
     * AWS_ACCESS_KEY_ID / AWS_SECRET_ACCESS_KEY 가 yml 에 박혀 있으면 static credentials,
     * 둘 다 비어있으면 DefaultCredentialsProvider (env / IAM role / ~/.aws) 로 fallback.
     */
    @Bean
    public S3Client s3Client(AwsProperties props) {
        AwsCredentialsProvider credentialsProvider =
                (StringUtils.hasText(props.accessKey()) && StringUtils.hasText(props.secretKey()))
                        ? StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(props.accessKey(), props.secretKey()))
                        : DefaultCredentialsProvider.create();

        return S3Client.builder()
                .region(Region.of(props.region()))
                .credentialsProvider(credentialsProvider)
                .build();
    }
}
