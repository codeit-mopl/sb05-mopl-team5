package com.mopl.api.global.config.image.impl;

import com.mopl.api.global.config.image.Uploader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Profile({"dev","test"})
@Component
public class LocalUploader implements Uploader {

    // TODO AWS S3 업로더를 구현하고 이 업로더와 설정에 따라 스위칭이 필요함(dev , prod)
    private final String UPLOAD_DIR = "uploads/contents/";

    public String upload(MultipartFile file) {
        try {
            String fileName = UUID.randomUUID() + "-" + file.getOriginalFilename();

            Path path = Paths.get(UPLOAD_DIR);
            Files.createDirectories(path);

            Path filePath = path.resolve(fileName);
            file.transferTo(filePath);

            return "contents/" + fileName;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
