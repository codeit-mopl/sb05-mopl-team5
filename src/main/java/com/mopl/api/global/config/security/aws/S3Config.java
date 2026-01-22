package com.mopl.api.global.config.security.aws;

import com.mopl.api.global.config.image.S3Uploader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class S3Config {

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
            .region(Region.AP_NORTHEAST_2)
                       .credentialsProvider(DefaultCredentialsProvider.create())
                       .build();
    }


}
