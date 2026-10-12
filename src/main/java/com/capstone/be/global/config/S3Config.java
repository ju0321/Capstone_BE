package com.capstone.be.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class S3Config {

    @Value("${cloud.aws.credentials.access-key}")
    private String accessKey;

    @Value("${cloud.aws.credentials.secret-key}")
    private String secretKey;

    @Value("${cloud.aws.s3.region}")
    private String region;

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(resolveCredentialsProvider())
                .build();
    }

    // 로컬/CI처럼 .env에 실제 Access Key가 없는 환경에서도 앱 구동 자체는 실패하지 않도록,
    // 값이 비어있으면 고정 자격증명을 만들지 않고 SDK 기본 자격증명 체인으로 대체한다.
    // (실제 업로드를 호출하는 시점에만 자격증명이 필요해지므로, 컨텍스트 로딩 테스트가
    // Access Key 없이도 통과할 수 있다.)
    private AwsCredentialsProvider resolveCredentialsProvider() {
        if (accessKey == null || accessKey.isBlank() || secretKey == null || secretKey.isBlank()) {
            return DefaultCredentialsProvider.create();
        }
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
    }
}
