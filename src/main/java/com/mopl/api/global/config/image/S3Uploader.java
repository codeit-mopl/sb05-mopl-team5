package com.mopl.api.global.config.image;

import com.mopl.api.domain.content.exception.detail.MissingFilenameException;
import com.mopl.api.global.config.image.Uploader;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Slf4j
@Profile("prod") // 🔥 핵심: prod 환경에서만 이 친구가 Uploader로 작동합니다.
@Component
@RequiredArgsConstructor
public class S3Uploader implements Uploader {

    private final S3Client s3Client;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;

    @Override
    public String upload(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw MissingFilenameException.WithFilename(originalFilename);
        }

        // 파일명 생성 (Local과 동일 로직)
        String fileName = "contents/" + UUID.randomUUID() + "_" + originalFilename;

        try {
            // S3 업로드
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                                                                .bucket(bucket)
                                                                .key(fileName)
                                                                .contentType(file.getContentType())
                                                                .build();

            s3Client.putObject(putObjectRequest,
                RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            // 업로드된 URL 반환
            return s3Client.utilities()
                           .getUrl(GetUrlRequest.builder().bucket(bucket).key(fileName).build())
                           .toString();

        } catch (IOException e) {
            log.error("S3 Upload Error", e);
            throw new RuntimeException("S3 업로드 실패", e);
        }
    }
}