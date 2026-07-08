package com.aiflow.enterprise.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.textract.TextractClient;

@Configuration
public class TextractConfig {

    @Value("${app.aws.access-key-id:}")
    private String accessKeyId;

    @Value("${app.aws.secret-access-key:}")
    private String secretAccessKey;

    @Value("${app.aws.region:us-east-1}")
    private String region;

    @Bean
    public TextractClient textractClient() {
        var builder = TextractClient.builder().region(Region.of(region));
        if (!accessKeyId.isBlank() && !secretAccessKey.isBlank()) {
            builder.credentialsProvider(
                    StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKeyId, secretAccessKey)));
        }
        return builder.build();
    }
}
